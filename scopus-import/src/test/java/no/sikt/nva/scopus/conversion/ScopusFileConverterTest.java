package no.sikt.nva.scopus.conversion;

import static no.sikt.nva.scopus.ScopusConstants.UPLOAD_DETAILS_USERNAME;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jdk.jfr.Description;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.UpwOaLocationType;
import no.scopus.generated.UpwOaLocationsType;
import no.scopus.generated.UpwOpenAccessType;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.conversion.files.TikaUtils;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import nva.commons.core.ioutils.IoUtils;
import org.apache.tika.io.TikaInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusFileConverterTest {

    public static final String DOI_PATH = "some/doi";
    public static final String HEADER_CONTENT_TYPE = "application/pdf";
    public static final String TEST_FILE_NAME = "test_file_name.xml";
    public static final URI DOWNLOAD_URL_FROM_CROSSREF_RESPONSE = URI.create(
        "https://www.cambridge.org/core/services/aop-cambridge-core/content/view/" + TEST_FILE_NAME);
    private HttpClient httpClient;
    private S3Client s3Client;
    private ScopusGenerator scopusData;
    private ScopusFileConverter fileConverter;

    @BeforeEach
    public void init() throws IOException, URISyntaxException {
        httpClient = mock(HttpClient.class);
        s3Client = mock(S3Client.class);
        var tikaUtils = mockedTikaUtils();
        fileConverter = new ScopusFileConverter(httpClient, s3Client, tikaUtils);

        scopusData = new ScopusGenerator();
        scopusData.getDocument().getMeta().setOpenAccess(null);
        scopusData.getDocument().getMeta().setDoi(DOI_PATH);
    }

    private TikaUtils mockedTikaUtils() throws IOException, URISyntaxException {
        var tikaUtils = mock(TikaUtils.class);
        var tikaInputStream = mock(TikaInputStream.class);
        when(tikaInputStream.getLength()).thenReturn(Long.parseLong(String.valueOf(randomInteger())));
        when(tikaInputStream.getPath()).thenReturn(
            Path.of(getClass().getClassLoader().getResource("2-s2.0-0000469852.xml").toURI()));
        when(tikaUtils.fetch(any())).thenReturn(tikaInputStream);
        when(tikaUtils.getMimeType(any())).thenReturn("application/pdf");
        return tikaUtils;
    }

    @Test
    void shouldCreateAssociatedArtifactWithEmbargoWhenSumOfDelayAndStartDateIsInFuture()
        throws IOException, InterruptedException {
        mockResponses("crossrefResponseWithEmbargo.json");
        var file = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();

        assertThat(file.getEmbargoDate().orElseThrow(), is(notNullValue()));
    }

    @Test
    void shouldRemoveDuplicateLinksWhenFetchingFilesFromCrossrefDoi() throws IOException, InterruptedException {
        mockResponses("crossrefResponse.json");

        var files = fileConverter.fetchAssociatedArtifacts(scopusData.getDocument());

        assertThat(files.size(), is(1));
    }

    @Test
    void shouldMapContentVersionToPublisherAuthority() throws IOException, InterruptedException {
        mockResponses("crossrefResponseWithEmbargo.json");

        var files = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();

        assertThat(files.getPublisherVersion(), is(PublisherVersion.PUBLISHED_VERSION));
    }

    @Test
    void shouldBeAbleToSetDefaultValuesToFileWhenDoiDoesNotContainEnoughData()
        throws IOException, InterruptedException {
        mockResponsesWithoutHeaders("crossrefResponseMissingFields.json");

        var files = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();

        assertThat(files.getPublisherVersion(), is(nullValue()));
        assertThat(files.getEmbargoDate(), is(Optional.empty()));
    }

    @Description("Resource primary url is a landing page url for resource and will never be a content file.")
    @Test
    void shouldNotCreateAssociatedArtifactFromLinkWhichAlsoIsAResourcePrimaryUrl()
        throws IOException, InterruptedException {
        mockResponses("crossrefResponseWithLinkAsResource.json");

        var files = fileConverter.fetchAssociatedArtifacts(scopusData.getDocument());

        assertThat(files, is(emptyIterable()));
    }

    @Test
    void shouldReturnSingleAssociatedArtifactsWhenMultipleArtifactsWithTheSameFileName()
        throws IOException, InterruptedException {
        var firstUrl = randomUri();
        var secondUrl = randomUri();
        scopusData.getDocument().getMeta().setOpenAccess(randomOpenAccessWithDownloadUrl(firstUrl, secondUrl));
        mockDownloadUrlResponse();

        var files = fileConverter.fetchAssociatedArtifacts(scopusData.getDocument());

        assertThat(files, hasSize(1));
    }

    @Test
    void shouldRemoveFileFromDoiWhenFileIsFromElseveierAndHasPlainTextContentType()
        throws IOException, InterruptedException {
        mockResponses("crossrefResponseWithElsveierFileToRemove.json");

        var files = fileConverter.fetchAssociatedArtifacts(scopusData.getDocument());

        assertThat(files, is(emptyIterable()));
    }

    @Test
    void shouldReturnEmptyListWhenFailingToCreateAssociatedArtifactFromXml() {
        var files = fileConverter.fetchAssociatedArtifacts(null);

        assertThat(files, is(emptyIterable()));
    }

    @Test
    void shouldCreateRandomFileNameWithFileTypeFromContentTypeHeaderWhenUrlAndContentDispositionMissingFileName()
        throws IOException, InterruptedException {
        var contentTypeHeader = Map.of(Header.CONTENT_TYPE, List.of("application/html"));
        mockResponsesWithHeader("crossrefResponseMissingFields.json", contentTypeHeader);
        var file = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();

        assertThat(file.getName(), containsString("html"));
    }

    @Test
    void shouldExtractFileNameFromDownloadUrlWhenContentDispositionHeaderMissesFileName()
        throws IOException, InterruptedException {
        var responseBody = "crossrefResponseMissingFields.json";
        mockResponsesWithoutHeaders(responseBody);
        var file = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();

        assertThat(file.getName(), is(equalTo(TEST_FILE_NAME)));
    }

    @Test
    void shouldCreateRandomPdfFileNameWhenCouldNotExtractFileNameOrFileTypeFromHeadersAndDownloadUrl()
        throws IOException, InterruptedException {
        var responseBody = "crossrefResponseMissingFields.json";
        mockResponsesWithHeader(responseBody, Map.of());
        var file = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();

        assertThat(file.getName(), is(notNullValue()));
    }

    @Test
    void shouldSetUploadDetailsWhenFetchingAssociatedArtifactsFromXml() throws IOException, InterruptedException {
        var firstUrl = randomUri();
        scopusData.getDocument().getMeta().setOpenAccess(randomOpenAccessWithDownloadUrl(firstUrl));
        mockDownloadUrlResponse();
        var file = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();
        var expectedUserName = UPLOAD_DETAILS_USERNAME;
        assertThat(file.getUploadDetails().getUploadedBy(), is(equalTo(expectedUserName)));
        assertThat(file.getUploadDetails().getUploadedDate(), is(notNullValue()));
    }

    @Test
    void shouldSetUploadDetailsWhenFetchingFileFromDoi() throws IOException, InterruptedException {
        var responseBody = "crossrefResponseMissingFields.json";
        mockResponsesWithHeader(responseBody, Map.of());
        var file = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).getFirst();
        var expectedUserName = UPLOAD_DETAILS_USERNAME;
        assertThat(file.getUploadDetails().getUploadedBy(), is(equalTo(expectedUserName)));
        assertThat(file.getUploadDetails().getUploadedDate(), is(notNullValue()));
    }

    private void mockDownloadUrlResponse() throws IOException, InterruptedException {
        var fetchDownloadUrlResponse = (HttpResponse<InputStream>) mock(HttpResponse.class);
        var body = mock(ByteArrayInputStream.class);
        when(body.available()).thenReturn(1000);
        when(body.readAllBytes()).thenReturn(randomString().getBytes());
        when(fetchDownloadUrlResponse.body()).thenReturn(body);
        when(fetchDownloadUrlResponse.headers()).thenReturn(createDownloadUrlHeaders());
        var request = mock(HttpRequest.class);
        when(request.uri()).thenReturn(randomUri());
        when(fetchDownloadUrlResponse.request()).thenReturn(request);
        when(fetchDownloadUrlResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(), eq(BodyHandlers.ofInputStream()))).thenReturn(fetchDownloadUrlResponse);
    }

    private UpwOaLocationType randomLocation(URI uri) {
        var location = new UpwOaLocationType();
        location.setUpwUrlForPdf(uri.toString());
        return location;
    }

    private OpenAccessType randomOpenAccessWithDownloadUrl(URI... uri) {
        var openAccess = new OpenAccessType();
        var upwOpenAccess = new UpwOpenAccessType();
        var locations = new UpwOaLocationsType();
        var locationList = Arrays.stream(uri).map(this::randomLocation).toList();
        locations.getUpwOaLocation().addAll(locationList);
        upwOpenAccess.setUpwOaLocations(locations);
        openAccess.setUpwOpenAccess(upwOpenAccess);
        return openAccess;
    }

    private void mockResponses(String responseBody) throws IOException, InterruptedException {
        var doiResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(doiResponse.body()).thenReturn(IoUtils.stringFromResources(Path.of(responseBody)));
        when(httpClient.send(any(), eq(BodyHandlers.ofString()))).thenReturn(doiResponse);

        var fetchDownloadUrlResponse = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(fetchDownloadUrlResponse.body()).thenReturn(new ByteArrayInputStream(randomString().getBytes()));
        when(fetchDownloadUrlResponse.headers()).thenReturn(createDownloadUrlHeaders());
        var request = mock(HttpRequest.class);
        when(request.uri()).thenReturn(randomUri());
        when(fetchDownloadUrlResponse.request()).thenReturn(request);
        when(fetchDownloadUrlResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(), eq(BodyHandlers.ofInputStream()))).thenReturn(fetchDownloadUrlResponse);
    }

    private void mockResponsesWithoutHeaders(String responseBody) throws IOException, InterruptedException {
        var doiResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(doiResponse.body()).thenReturn(IoUtils.stringFromResources(Path.of(responseBody)));
        when(httpClient.send(any(), eq(BodyHandlers.ofString()))).thenReturn(doiResponse);

        var fetchDownloadUrlResponse = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(fetchDownloadUrlResponse.body()).thenReturn(new ByteArrayInputStream(randomString().getBytes()));
        when(fetchDownloadUrlResponse.headers()).thenReturn(emptyHeaders());
        var req = mock(HttpRequest.class);
        when(req.uri()).thenReturn(randomUri());
        when(fetchDownloadUrlResponse.request()).thenReturn(req);
        when(fetchDownloadUrlResponse.statusCode()).thenReturn(200);
        var request = HttpRequest.newBuilder().uri(DOWNLOAD_URL_FROM_CROSSREF_RESPONSE).build();
        when(fetchDownloadUrlResponse.request()).thenReturn(request);
        when(httpClient.send(any(), eq(BodyHandlers.ofInputStream()))).thenReturn(fetchDownloadUrlResponse);
    }

    private void mockResponsesWithHeader(String responseBody, Map<String, List<String>> header)
        throws IOException, InterruptedException {
        var doiResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(doiResponse.body()).thenReturn(IoUtils.stringFromResources(Path.of(responseBody)));
        when(httpClient.send(any(), eq(BodyHandlers.ofString()))).thenReturn(doiResponse);

        var fetchDownloadUrlResponse = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(fetchDownloadUrlResponse.body()).thenReturn(new ByteArrayInputStream(randomString().getBytes()));
        when(fetchDownloadUrlResponse.headers()).thenReturn(HttpHeaders.of(header, (s, s2) -> true));
        var req = mock(HttpRequest.class);
        when(req.uri()).thenReturn(randomUri()).thenReturn(null);
        when(fetchDownloadUrlResponse.request()).thenReturn(req);
        when(fetchDownloadUrlResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(), eq(BodyHandlers.ofInputStream()))).thenReturn(fetchDownloadUrlResponse);
    }

    private HttpHeaders createDownloadUrlHeaders() {
        return HttpHeaders.of(Map.of("Content-Type", List.of(HEADER_CONTENT_TYPE), "Content-Disposition",
                                     List.of("attachment; filename=\"someFile\"")), (s, s2) -> true);
    }

    private HttpHeaders emptyHeaders() {
        return HttpHeaders.of(Map.of(), (s, s2) -> true);
    }
}
