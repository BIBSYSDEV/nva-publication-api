package no.sikt.nva.scopus;

import static java.util.UUID.randomUUID;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.s3.Headers;
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
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ScopusFileConverter {

    private static final String CONTENT_DISPOSITION_FILE_NAME_PATTERN = "filename=\"%s\"";
    public static final String IMPORT_CANDIDATES_FILES_BUCKET = new Environment().readEnv(
        "IMPORT_CANDIDATES_STORAGE_BUCKET");
    private static final URI RIGHTS_RESERVED_LICENSE = URI.create("https://creativecommons.org/licenses/by/4.0/");
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_TYPE_DELIMITER = ";";
    public static final String FILE_TYPE_DELIMITER = "/";
    public static final String FILENAME = "filename";
    public static final String PDF_FILE_TYPE = "pdf";
    public static final String FILE_NAME_DELIMITER = ".";
    private final HttpClient httpClient;
    private final S3Client s3Client;

    public ScopusFileConverter(HttpClient httpClient, S3Client s3Client) {
        this.httpClient = httpClient;
        this.s3Client = s3Client;
    }

    public List<AssociatedArtifact> fetchAssociatedArtifacts(DocTp docTp) {
        return getLocations(docTp).stream()
                   .map(UpwOaLocationType::getUpwUrlForPdf)
                   .distinct()
                   .filter(Objects::nonNull)
                   .map(this::convertToAssociatedArtifact)
                   .toList();
    }

    //TODO: Fetched files should be scanned for malware
    public AssociatedArtifact convertToAssociatedArtifact(String downloadUrl) {
        var response = fetchResponse(downloadUrl);
        var fileIdentifier = randomUUID();
        var filename = getFilename(response);
        saveFile(filename, fileIdentifier, response);
        var head = fetchFileInfo(fileIdentifier);
        return File.builder()
                   .withIdentifier(fileIdentifier)
                   .withName(filename)
                   .withMimeType(head.contentType())
                   .withSize(head.contentLength())
                   .withLicense(RIGHTS_RESERVED_LICENSE)
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

    private static HttpRequest constructRequest(String uri) {
        return HttpRequest.newBuilder().GET().uri(URI.create(uri)).build();
    }

    private static String getContentType(HttpResponse<InputStream> response) {
        return Optional.of(response.headers().firstValue(Headers.CONTENT_TYPE))
                   .map(Optional::orElseThrow)
                   .map(value -> value.split(CONTENT_TYPE_DELIMITER)[0])
                   .orElse(CONTENT_TYPE_OCTET_STREAM);
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

    private HttpResponse<InputStream> fetchResponse(String type) {
        return attempt(
            () -> httpClient.send(constructRequest(type), BodyHandlers.ofInputStream())).orElseThrow();
    }

    private List<UpwOaLocationType> getLocations(DocTp docTp) {
        return Optional.ofNullable(docTp.getMeta().getOpenAccess())
                   .map(OpenAccessType::getUpwOpenAccess)
                   .map(UpwOpenAccessType::getUpwOaLocations)
                   .map(UpwOaLocationsType::getUpwOaLocation)
                   .orElse(List.of());
    }
}
