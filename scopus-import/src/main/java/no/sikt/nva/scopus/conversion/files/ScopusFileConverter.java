package no.sikt.nva.scopus.conversion.files;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static no.sikt.nva.scopus.conversion.files.model.ContentVersion.AM;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collector;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.CrossrefLink;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.License;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Message;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Primary;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Resource;
import no.sikt.nva.scopus.conversion.files.model.ScopusFile;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.entity.ContentType;
import org.apache.tika.io.TikaInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class ScopusFileConverter {

    public static final String IMPORT_CANDIDATES_FILES_BUCKET = new Environment().readEnv(
        "IMPORT_CANDIDATES_STORAGE_BUCKET");
    public static final String CONTENT_TYPE_DELIMITER = ";";
    public static final String PDF_FILE_TYPE = "pdf";
    public static final String FILE_NAME_DELIMITER = ".";
    public static final String CROSSREF_URI_ENV_VAR_NAME = "CROSSREF_FETCH_DOI_URI";
    public static final String CROSSREF_DEFAULT_URI = "https://api.crossref.org/v1/works/";
    public static final String FETCH_FILE_FROM_DOI_ERROR_MESSAGE = "Could not fetch file from doi: {}";
    public static final String CREATIVE_COMMONS_DOMAIN = "creativecommons.org";
    public static final String HTML_CONTENT_TYPE = "text/html";
    public static final String XML_CONTENT_TYPE = "text/xml";
    public static final String FILENAME_CONTENT_TYPE_HEADER_VALUE = "filename=";
    public static final String QUOTE = "\"";
    public static final String ELSEVIER_HOST = "api.elsevier.com";
    public static final String FETCH_FILE_ERROR_MESSAGE = "Could not fetch file. %s";
    public static final String COULD_NOT_SAVE_FILE = "Could not save file to s3 {}";
    private static final Logger logger = LoggerFactory.getLogger(ScopusFileConverter.class);
    private static final String CONTENT_DISPOSITION_FILE_NAME_PATTERN = "filename=\"%s\"";
    public final String crossRefUri;
    private final HttpClient httpClient;
    private final S3Client s3Client;
    private final TikaUtils tikaUtils;

    @JacocoGenerated
    public ScopusFileConverter(HttpClient httpClient, S3Client s3Client, TikaUtils tikaUtils) {
        this.httpClient = httpClient;
        this.s3Client = s3Client;
        this.crossRefUri = new Environment().readEnvOpt(CROSSREF_URI_ENV_VAR_NAME).orElse(CROSSREF_DEFAULT_URI);
        this.tikaUtils = tikaUtils;
    }

    public ScopusFileConverter(HttpClient httpClient, S3Client s3Client, Environment environment, TikaUtils tikaUtils) {
        this.httpClient = httpClient;
        this.s3Client = s3Client;
        this.crossRefUri = environment.readEnv(CROSSREF_URI_ENV_VAR_NAME);
        this.tikaUtils = tikaUtils;
    }

    public List<AssociatedArtifact> fetchAssociatedArtifacts(DocTp docTp) {
        return fetchFilesFromDoi(docTp).orElseGet(List::of);
    }

    private static CrossrefResponse toCrossrefResponse(String body) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(body, CrossrefResponse.class);
    }

    private static String getFilename(HttpResponse<InputStream> response) {
        return response.headers()
                   .firstValue(Headers.CONTENT_DISPOSITION)
                   .map(ScopusFileConverter::extractFileNameFromContentDisposition)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .or(() -> extractFileNameFromUrl(response))
                   .or(() -> randomFileNameWithMimeTypeFromContentTypeHeader(response.headers()))
                   .orElseGet(ScopusFileConverter::randomStringPdfFileName);
    }

    private static String randomStringPdfFileName() {
        return fileNameWithExtension(PDF_FILE_TYPE);
    }

    private static Optional<String> randomFileNameWithMimeTypeFromContentTypeHeader(HttpHeaders headers) {
        return headers.firstValue(Header.CONTENT_TYPE)
                   .map(ScopusFileConverter::extractFileExtensionFromContentType)
                   .map(ScopusFileConverter::fileNameWithExtension);
    }

    private static String fileNameWithExtension(String fileExtension) {
        return randomUUID() + FILE_NAME_DELIMITER + fileExtension;
    }

    private static String extractFileExtensionFromContentType(String value) {
        return value.split("/")[1];
    }

    private static Optional<String> extractFileNameFromUrl(HttpResponse<InputStream> response) {
        return Optional.ofNullable(response.request())
                   .map(HttpRequest::uri)
                   .map(URI::getPath)
                   .map(Paths::get)
                   .map(Path::getFileName)
                   .map(Path::toString);
    }

    private static Optional<String> extractFileNameFromContentDisposition(String contentType) {
        return attempt(() -> contentType.split(FILENAME_CONTENT_TYPE_HEADER_VALUE)[1]
                                 .split(CONTENT_TYPE_DELIMITER)[0]
                                 .replace(QUOTE, StringUtils.EMPTY_STRING)).toOptional();
    }

    private static HttpRequest constructRequest(URI uri) {
        return HttpRequest.newBuilder().GET().uri(uri).build();
    }

    private static LocalDate toEmbargoDate(License license) {
        return LocalDate.of(license.getStart().getYear(), license.getStart().getMonth(), license.getStart().getDay())
                   .plusDays(license.getDelay());
    }

    private static URI extractLicenseForLink(CrossrefLink crossrefLink, List<License> licenseList) {
        return getLicenseWithTheSameVersion(crossrefLink, licenseList).or(
            () -> getLicenseWithUnspecifiedContentVersion(licenseList)).orElse(null);
    }

    private static Optional<URI> getLicenseWithUnspecifiedContentVersion(List<License> licenseList) {
        return licenseList.stream()
                   .filter(License::hasUnspecifiedContentVersion)
                   .map(License::getUri)
                   .filter(ScopusFileConverter::isCreativeCommonsLicense)
                   .findFirst();
    }

    private static Optional<URI> getLicenseWithTheSameVersion(CrossrefLink crossrefLink, List<License> licenseList) {
        return licenseList.stream()
                   .filter(license -> hasSameVersion(crossrefLink, license))
                   .map(License::getUri)
                   .filter(ScopusFileConverter::isCreativeCommonsLicense)
                   .findFirst();
    }

    private static boolean isSuccess(HttpResponse<InputStream> response) {
        return response.statusCode() == HTTP_OK;
    }

    private static Collector<ScopusFile, Object, ArrayList<ScopusFile>> collectRemovingDuplicates() {
        return collectingAndThen(toCollection(() -> new TreeSet<>(comparing(ScopusFile::downloadFileUrl))),
                                 ArrayList::new);
    }

    private static boolean isCreativeCommonsLicense(URI uri) {
        return uri.toString().contains(CREATIVE_COMMONS_DOMAIN);
    }

    private static boolean hasSameVersion(CrossrefLink crossrefLink, License license) {
        return license.getContentVersion().equals(crossrefLink.getContentVersion());
    }

    private static boolean isElsevierPlainTextResource(CrossrefLink crossrefLink) {
        return ELSEVIER_HOST.equals(crossrefLink.getUri().getHost()) &&
               crossrefLink.getContentType().equals(ContentType.TEXT_PLAIN.getMimeType());
    }

    private Optional<List<AssociatedArtifact>> fetchFilesFromDoi(DocTp docTp) {
        try {
            var response = fetchDoi(docTp.getMeta().getDoi());
            var associatedArtifacts = fetchFilesFromDoi(response);
            return associatedArtifacts.isEmpty() ? Optional.empty() : Optional.of(associatedArtifacts);
        } catch (Exception e) {
            logger.info(FETCH_FILE_FROM_DOI_ERROR_MESSAGE, e.getMessage());
            return Optional.empty();
        }
    }

    private List<AssociatedArtifact> fetchFilesFromDoi(CrossrefResponse response) {
        return getFilesFromCrossrefResponse(response).stream()
                   .map(this::fetchScopusFile)
                   .collect(collectRemovingDuplicates())
                   .stream()
                   .map(this::saveFile)
                   .filter(ScopusFile::isValid)
                   .map(ScopusFile::toFile)
                   .toList();
    }

    private ScopusFile fetchScopusFile(ScopusFile scopusFile) {
        return attempt(() -> fetchFileContent(scopusFile)).orElseThrow();
    }

    private ScopusFile fetchFileContent(ScopusFile file) throws IOException {
        var response = fetchResponseAsInputStream(file.downloadFileUrl());
        var inputStream = TikaInputStream.get(response.body());
        var filename = getFilename(response);
        var size = inputStream.getLength();
        return file.copy().withContent(inputStream).withSize(size).withName(filename).build();
    }

    private HttpResponse<InputStream> fetchResponseAsInputStream(URI uri) {
        var response = attempt(
            () -> httpClient.send(constructRequest(uri), BodyHandlers.ofInputStream())).orElseThrow();
        if (isSuccess(response)) {
            return response;
        } else {
            throw new RuntimeException(FETCH_FILE_ERROR_MESSAGE.formatted(response.toString()));
        }
    }

    private List<ScopusFile> getFilesFromCrossrefResponse(CrossrefResponse response) {
        var licenseList = extractLicenses(response);
        var resource = extractResourceUri(response);
        return response.getMessage()
                   .getLinks()
                   .stream()
                   .filter(crossrefLink -> isCrossRefLinkToImport(crossrefLink, resource))
                   .map(crossrefLink -> toScopusFile(crossrefLink, licenseList))
                   .distinct()
                   .toList();
    }

    private boolean isCrossRefLinkToImport(CrossrefLink crossrefLink, URI resource) {
        return hasSupportedMimeType(crossrefLink) && shouldNotBeIgnored(crossrefLink) &&
               isNotResource(crossrefLink, resource);
    }

    private boolean shouldNotBeIgnored(CrossrefLink crossrefLink) {
        return !isElsevierPlainTextResource(crossrefLink);
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

    private ScopusFile toScopusFile(CrossrefLink crossrefLink, List<License> licenseList) {
        return ScopusFile.builder()
                   .withIdentifier(randomUUID())
                   .withDownloadFileUrl(crossrefLink.getUri())
                   .withPublisherVersion(determinePublisherVersion(crossrefLink))
                   .withLicense(extractLicenseForLink(crossrefLink, licenseList))
                   .withEmbargo(calculateEmbargo(crossrefLink, licenseList))
                   .build();
    }

    private PublisherVersion determinePublisherVersion(CrossrefLink crossrefLink) {
        if (VOR.equals(crossrefLink.getContentVersion())) {
            return PublisherVersion.PUBLISHED_VERSION;
        } else if (AM.equals(crossrefLink.getContentVersion())) {
            return PublisherVersion.ACCEPTED_VERSION;
        } else {
            return null;
        }
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

    private boolean hasSupportedMimeType(CrossrefLink link) {
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

    private ScopusFile saveFile(ScopusFile scopusFile) {
        try {
            var path = scopusFile.content().getPath();
            var mimeType = tikaUtils.getMimeType(scopusFile.content());
            s3Client.putObject(PutObjectRequest.builder()
                                   .bucket(IMPORT_CANDIDATES_FILES_BUCKET)
                                   .contentDisposition(
                                       String.format(CONTENT_DISPOSITION_FILE_NAME_PATTERN, scopusFile.name()))
                                   .contentType(mimeType)
                                   .key(scopusFile.identifier().toString())
                                   .build(), RequestBody.fromFile(path));
            return scopusFile.copy().withMimeType(mimeType).build();
        } catch (Exception e) {
            logger.error(COULD_NOT_SAVE_FILE, e.getMessage());
            return scopusFile;
        }
    }
}