package no.sikt.nva.scopus;

import static java.util.UUID.randomUUID;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.s3.Headers;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import no.scopus.generated.DocTp;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.UpwOaLocationType;
import no.scopus.generated.UpwOaLocationsType;
import no.scopus.generated.UpwOpenAccessType;
import no.sikt.nva.scopus.CrossrefResponse.CrossrefLink;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ScopusFileConverter {

    public static final String IMPORT_CANDIDATES_FILES_BUCKET = new Environment().readEnv(
        "IMPORT_CANDIDATES_STORAGE_BUCKET");
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_TYPE_DELIMITER = ";";
    public static final String FILE_TYPE_DELIMITER = "/";
    public static final String FILENAME = "filename";
    public static final String PDF_FILE_TYPE = "pdf";
    public static final String FILE_NAME_DELIMITER = ".";
    public static final String CROSSREF_URI_ENV_VAR_NAME = "CROSSREF_FETCH_DOI_URI";
    public static final String CROSSREF_DEFAULT_URI = "https://api.crossref.org/v1/works/";
    public static final String FETCH_FILE_FROM_XML_MESSAGE_ERROR_MESSAGE = "Could not fetch file from xml: {}";
    public static final String FETCH_FILE_FROM_DOI_ERROR_MESSAGE = "Could not fetch file from doi: {}";
    private static final Logger logger = LoggerFactory.getLogger(ScopusFileConverter.class);
    private static final String CONTENT_DISPOSITION_FILE_NAME_PATTERN = "filename=\"%s\"";
    private static final URI DEFAULT_LICENSE = URI.create("https://creativecommons.org/licenses/by/4.0/");
    public final String crossRefUri;
    private final HttpClient httpClient;
    private final S3Client s3Client;

    @JacocoGenerated
    public ScopusFileConverter(HttpClient httpClient, S3Client s3Client) {
        this.httpClient = httpClient;
        this.s3Client = s3Client;
        this.crossRefUri = new Environment().readEnvOpt(CROSSREF_URI_ENV_VAR_NAME).orElse(CROSSREF_DEFAULT_URI);
    }

    public ScopusFileConverter(HttpClient httpClient, S3Client s3Client, Environment environment) {
        this.httpClient = httpClient;
        this.s3Client = s3Client;
        this.crossRefUri = environment.readEnv("CROSSREF_FETCH_DOI_URI");
    }

    public List<AssociatedArtifact> fetchAssociatedArtifacts(DocTp docTp) {
        var associatedArtifactsFromXmlReferences = extractAssociatedArtifactsFromFileReference(docTp);
        return !associatedArtifactsFromXmlReferences.isEmpty()
                   ? associatedArtifactsFromXmlReferences
                   : extractAssociatedArtifactsFromDoi(docTp);
    }

    private static CrossrefResponse toCrossrefResponse(String body) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(body, CrossrefResponse.class);
    }

    private static File createFile(UUID fileIdentifier, String filename, HeadObjectResponse head) {
        return File.builder()
                   .withIdentifier(fileIdentifier)
                   .withName(filename)
                   .withMimeType(head.contentType())
                   .withSize(head.contentLength())
                   .withLicense(DEFAULT_LICENSE)
                   .buildPublishedFile();
    }

    private static String getFilename(HttpResponse<InputStream> response) {
        return Optional.ofNullable(response.headers().map().get(Headers.CONTENT_DISPOSITION))
                   .map(list -> list.stream().filter(item -> item.contains(FILENAME)).toList())
                   .map(list -> list.get(0))
                   .map(ScopusFileConverter::getFilename)
                   .orElse(randomUUID() + FILE_NAME_DELIMITER + getFileType(response));
    }

    private static String getFilename(String value) {
        return value.split("filename=")[1].split(CONTENT_TYPE_DELIMITER)[0].replace("\"", StringUtils.EMPTY_STRING);
    }

    private static String getFileType(HttpResponse<InputStream> response) {
        return Optional.ofNullable(response.headers())
                   .map(HttpHeaders::map)
                   .map(map -> map.get(Headers.CONTENT_TYPE))
                   .map(values -> values.stream().filter(value -> value.contains("application")).toList())
                   .map(list -> list.get(0))
                   .map(item -> item.split(FILE_TYPE_DELIMITER)[1])
                   .orElse(PDF_FILE_TYPE);
    }

    private static HttpRequest constructRequest(URI uri) {
        return HttpRequest.newBuilder().GET().uri(uri).build();
    }

    private static String getContentType(HttpResponse<InputStream> response) {
        return Optional.of(response.headers().firstValue(Headers.CONTENT_TYPE))
                   .map(Optional::orElseThrow)
                   .map(value -> value.split(CONTENT_TYPE_DELIMITER)[0])
                   .orElse(CONTENT_TYPE_OCTET_STREAM);
    }

    private List<AssociatedArtifact> extractAssociatedArtifactsFromDoi(DocTp docTp) {
        try {
            var doi = docTp.getMeta().getDoi();
            return fetchDoi(doi).getMessage()
                       .getLinks()
                       .stream()
                       .map(CrossrefLink::getUri)
                       .filter(Objects::nonNull)
                       .map(this::convertToAssociatedArtifact)
                       .filter(Optional::isPresent)
                       .map(Optional::get)
                       .toList();
        } catch (Exception e) {
            logger.info(FETCH_FILE_FROM_DOI_ERROR_MESSAGE, e.getMessage());
            return List.of();
        }
    }

    private CrossrefResponse fetchDoi(String doi) {
        return attempt(() -> constructCrossrefDoiUri(doi))
                   .map(ScopusFileConverter::constructRequest)
                   .map(this::fetchResponseAsString)
                   .map(HttpResponse::body)
                   .map(ScopusFileConverter::toCrossrefResponse)
                   .orElseThrow();
    }

    private HttpResponse<String> fetchResponseAsString(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private URI constructCrossrefDoiUri(String doi) {
        return UriWrapper.fromUri(crossRefUri).addChild(doi).getUri();
    }

    //TODO: Fetched files should be scanned for malware
    private Optional<AssociatedArtifact> convertToAssociatedArtifact(URI downloadUrl) {
        try {
            var response = fetchResponseAsInputStream(downloadUrl);
            var fileIdentifier = randomUUID();
            var filename = getFilename(response);
            saveFile(filename, fileIdentifier, response);
            var head = fetchFileInfo(fileIdentifier);
            return Optional.of(createFile(fileIdentifier, filename, head));
        } catch (Exception e) {
            logger.error(FETCH_FILE_FROM_XML_MESSAGE_ERROR_MESSAGE, e.getMessage());
            return Optional.empty();
        }
    }

    private List<AssociatedArtifact> extractAssociatedArtifactsFromFileReference(DocTp docTp) {
        return getLocations(docTp).stream()
                   .map(UpwOaLocationType::getUpwUrlForPdf)
                   .distinct()
                   .filter(Objects::nonNull)
                   .map(URI::create)
                   .map(this::convertToAssociatedArtifact)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .toList();
    }

    private HeadObjectResponse fetchFileInfo(UUID fileIdentifier) {
        var request = HeadObjectRequest.builder()
                          .bucket(IMPORT_CANDIDATES_FILES_BUCKET)
                          .key(fileIdentifier.toString())
                          .build();
        return s3Client.headObject(request);
    }

    private void saveFile(String fileName, UUID fileIdentifier, HttpResponse<InputStream> response) {
        var fileToSave = attempt(() -> response.body().readAllBytes()).orElseThrow();
        s3Client.putObject(PutObjectRequest.builder()
                               .bucket(IMPORT_CANDIDATES_FILES_BUCKET)
                               .contentDisposition(String.format(CONTENT_DISPOSITION_FILE_NAME_PATTERN, fileName))
                               .contentType(getContentType(response))
                               .key(fileIdentifier.toString())
                               .build(), RequestBody.fromBytes(fileToSave));
    }

    private HttpResponse<InputStream> fetchResponseAsInputStream(URI uri) {
        return attempt(() -> httpClient.send(constructRequest(uri), BodyHandlers.ofInputStream())).orElseThrow();
    }

    private List<UpwOaLocationType> getLocations(DocTp docTp) {
        return Optional.ofNullable(docTp.getMeta().getOpenAccess())
                   .map(OpenAccessType::getUpwOpenAccess)
                   .map(UpwOpenAccessType::getUpwOaLocations)
                   .map(UpwOaLocationsType::getUpwOaLocation)
                   .orElse(List.of());
    }
}
