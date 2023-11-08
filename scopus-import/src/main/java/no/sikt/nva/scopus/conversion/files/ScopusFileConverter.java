package no.sikt.nva.scopus.conversion.files;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static no.sikt.nva.scopus.conversion.files.model.ContentVersion.VOR;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collector;
import no.scopus.generated.DocTp;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.UpwOaLocationType;
import no.scopus.generated.UpwOaLocationsType;
import no.scopus.generated.UpwOpenAccessType;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.CrossrefLink;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.License;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Message;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Primary;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Resource;
import no.sikt.nva.scopus.conversion.files.model.ScopusFile;
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
    public static final String DEFAULT_CONTENT_TYPE = "application/pdf";
    public static final String CONTENT_TYPE_DELIMITER = ";";
    public static final String FILE_TYPE_DELIMITER = "/";
    public static final String FILENAME = "filename";
    public static final String PDF_FILE_TYPE = "pdf";
    public static final String FILE_NAME_DELIMITER = ".";
    public static final String CROSSREF_URI_ENV_VAR_NAME = "CROSSREF_FETCH_DOI_URI";
    public static final String CROSSREF_DEFAULT_URI = "https://api.crossref.org/v1/works/";
    public static final String FETCH_FILE_FROM_XML_MESSAGE_ERROR_MESSAGE = "Could not fetch file from xml: {}";
    public static final String FETCH_FILE_FROM_DOI_ERROR_MESSAGE = "Could not fetch file from doi: {}";
    public static final String CREATIVECOMMONS_DOMAIN = "creativecommons.org";
    public static final String HTML_CONTENT_TYPE = "text/html";
    public static final String XML_CONTENT_TYPE = "text/xml";
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
        return !associatedArtifactsFromXmlReferences.isEmpty() ? associatedArtifactsFromXmlReferences
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
                   .map(optional -> optional.orElse(null))
                   .map(value -> value.split(CONTENT_TYPE_DELIMITER)[0])
                   .orElse(DEFAULT_CONTENT_TYPE);
    }

    private static LocalDate toEmbargoDate(License license) {
        return LocalDate.of(license.getStart().getYear(), license.getStart().getMonth(), license.getStart().getDay())
                   .plusDays(license.getDelay());
    }

    private static URI extractLicenseForLink(CrossrefLink crossrefLink, List<License> licenses) {
        return licenses.stream()
                   .filter(license -> hasSameVersion(crossrefLink, license))
                   .map(License::getUri)
                   .filter(ScopusFileConverter::isCreativeCommonsLicense)
                   .findFirst()
                   .orElse(DEFAULT_LICENSE);
    }

    private static boolean isSuccess(HttpResponse<InputStream> response) {
        return response.statusCode() == HTTP_OK;
    }

    private static Collector<ScopusFile, Object, ArrayList<ScopusFile>> collectRemovingDuplicates() {
        return collectingAndThen(toCollection(() -> new TreeSet<>(comparing(ScopusFile::downloadFileUrl))),
                                 ArrayList::new);
    }

    private static boolean isCreativeCommonsLicense(URI uri) {
        return uri.toString().contains(CREATIVECOMMONS_DOMAIN);
    }

    private List<AssociatedArtifact> extractAssociatedArtifactsFromDoi(DocTp docTp) {
        try {
            var doi = docTp.getMeta().getDoi();
            var response = fetchDoi(doi);
            return getScopusFiles(response).stream()
                       .map(s -> attempt(() -> fetchFileContent(s)).orElseThrow())
                       .collect(collectRemovingDuplicates())
                       .stream()
                       .map(this::saveFile)
                       .map(ScopusFile::toPublishedAssociatedArtifact)
                       .toList();
        } catch (Exception e) {
            logger.info(FETCH_FILE_FROM_DOI_ERROR_MESSAGE, e.getMessage());
            return List.of();
        }
    }

    private ScopusFile fetchFileContent(ScopusFile file) throws IOException {
        var fetchFileResponse = fetchResponseAsInputStream(file.downloadFileUrl());
        var content = fetchFileResponse.body();
        return file.copy()
                   .withContent(content)
                   .withSize(content.available())
                   .withName(getFilename(fetchFileResponse))
                   .build();
    }

    private List<ScopusFile> getScopusFiles(CrossrefResponse response) {
        var licenses = extractLicenses(response);
        var resource = extractResourceUri(response);
        return response.getMessage()
                   .getLinks()
                   .stream()
                   .filter(this::hasSupportedContentType)
                   .filter(crossrefLink -> isNotResource(crossrefLink, resource))
                   .map(crossrefLink -> toScopusFile(crossrefLink, licenses))
                   .distinct()
                   .toList();
    }

    private boolean isNotResource(CrossrefLink crossrefLink, URI resource) {
        return isNull(resource) || !crossrefLink.getUri().equals(resource);
    }

    private URI extractResourceUri(CrossrefResponse response) {
        return Optional.ofNullable(response.getMessage())
                   .map(Message::getResource)
                   .map(Resource::getPrimary)
                   .map(Primary::getUri)
                   .orElse(null);
    }

    private ScopusFile toScopusFile(CrossrefLink crossrefLink, List<License> licenses) {
        return ScopusFile.builder()
                   .withIdentifier(randomUUID())
                   .withDownloadFileUrl(crossrefLink.getUri())
                   .withPublisherAuthority(VOR.equals(crossrefLink.getContentVersion()))
                   .withLicense(extractLicenseForLink(crossrefLink, licenses))
                   .withEmbargo(calculateEmbargo(crossrefLink, licenses))
                   .withContentType(crossrefLink.getContentType())
                   .build();
    }

    private Instant calculateEmbargo(CrossrefLink crossrefLink, List<License> licenses) {
        return licenses.stream()
                   .filter(license -> hasSameVersion(crossrefLink, license))
                   .findFirst()
                   .map(this::calculateEmbargo)
                   .orElse(null);
    }

    private Instant calculateEmbargo(License license) {
        if (license.hasDelay()) {
            var embargoDate = toEmbargoDate(license).atStartOfDay(ZoneId.systemDefault()).toInstant();
            return embargoDate.isAfter(Instant.now()) ? embargoDate : null;
        }
        return null;
    }

    private static boolean hasSameVersion(CrossrefLink crossrefLink, License license) {
        return license.getContentVersion().equals(crossrefLink.getContentVersion());
    }

    private boolean hasSupportedContentType(CrossrefLink link) {
        var contentType = link.getContentType();
        return nonNull(contentType) && !HTML_CONTENT_TYPE.equals(contentType) && !XML_CONTENT_TYPE.equals(contentType);
    }

    private List<License> extractLicenses(CrossrefResponse doiResponse) {
        return Optional.ofNullable(doiResponse.getMessage().getLicense()).orElse(List.of());
    }

    private CrossrefResponse fetchDoi(String doi) {
        return attempt(() -> constructCrossrefDoiUri(doi)).map(ScopusFileConverter::constructRequest)
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
            return convertToAssociatedArtifact(response);
        } catch (Exception e) {
            logger.error(FETCH_FILE_FROM_XML_MESSAGE_ERROR_MESSAGE, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AssociatedArtifact> convertToAssociatedArtifact(HttpResponse<InputStream> response) {
        if (isSuccess(response)) {
            var fileIdentifier = randomUUID();
            var filename = getFilename(response);
            saveFile(filename, fileIdentifier, response);
            var head = fetchFileInfo(fileIdentifier);
            return Optional.of(createFile(fileIdentifier, filename, head));
        }
        throw new RuntimeException();
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

    private ScopusFile saveFile(ScopusFile scopusFile) {
        var content = attempt(() -> scopusFile.content().readAllBytes()).orElseThrow();
        s3Client.putObject(PutObjectRequest.builder()
                               .bucket(IMPORT_CANDIDATES_FILES_BUCKET)
                               .contentDisposition(
                                   String.format(CONTENT_DISPOSITION_FILE_NAME_PATTERN, scopusFile.name()))
                               .contentType(
                                   nonNull(scopusFile.contentType()) ? scopusFile.contentType() : DEFAULT_CONTENT_TYPE)
                               .key(scopusFile.identifier().toString())
                               .build(), RequestBody.fromBytes(content));
        return scopusFile;
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
