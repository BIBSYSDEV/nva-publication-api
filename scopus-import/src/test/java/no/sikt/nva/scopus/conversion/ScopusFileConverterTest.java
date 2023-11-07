package no.sikt.nva.scopus.conversion;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

public class ScopusFileConverterTest {

    public static final String DOI_PATH = "some/doi";
    private HttpClient httpClient;
    private S3Client s3Client;
    private ScopusGenerator scopusData;
    private ScopusFileConverter fileConverter;

    @BeforeEach
    public void init() {
        httpClient = mock(HttpClient.class);
        s3Client = mock(S3Client.class);
        scopusData = new ScopusGenerator();
        fileConverter = new ScopusFileConverter(httpClient, s3Client);
        fileConverter = new ScopusFileConverter(httpClient, s3Client);
    }

    @Test
    void shouldCreateAssociatedArtifactWithEmbargoWhenSumOfDelayAndStartDateIsInFuture()
        throws IOException, InterruptedException {
        var scopusData = new ScopusGenerator();
        scopusData.getDocument().getMeta().setOpenAccess(null);
        scopusData.getDocument().getMeta().setDoi(DOI_PATH);
        mockResponses("crossrefResponseWithEmbargo.json");
        var file = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).stream().map(i -> (PublishedFile) i).toList().get(0);

        assertThat(file.getEmbargoDate().orElseThrow(), is(notNullValue()));
    }

    @Test
    void shouldRemoveDuplicateLinksWhenFetchingFilesFromCrossrefDoi() throws IOException, InterruptedException {
        var scopusData = new ScopusGenerator();
        scopusData.getDocument().getMeta().setOpenAccess(null);
        scopusData.getDocument().getMeta().setDoi(DOI_PATH);
        mockResponses("crossrefResponse.json");
        var files = fileConverter.fetchAssociatedArtifacts(scopusData.getDocument());

        assertThat(files.size(), is(1));
    }

    @Test
    void shouldMapContentVersionToPublisherAuthority() throws IOException, InterruptedException {
        var scopusData = new ScopusGenerator();
        scopusData.getDocument().getMeta().setOpenAccess(null);
        scopusData.getDocument().getMeta().setDoi(DOI_PATH);
        mockResponses("crossrefResponseWithEmbargo.json");
        var files = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).get(0);

        assertThat(files.isPublisherAuthority(), is(true));
    }

    @Test
    void shouldBeAbleToSetDefaultValuesToFileWhenDoiDoesNotContainEnoughData() throws IOException,
                                                                                      InterruptedException {
        var scopusData = new ScopusGenerator();
        scopusData.getDocument().getMeta().setOpenAccess(null);
        scopusData.getDocument().getMeta().setDoi(DOI_PATH);
        mockResponses("crossrefResponseMissingFields.json");
        var files = (PublishedFile) fileConverter.fetchAssociatedArtifacts(scopusData.getDocument()).get(0);

        assertThat(files.isPublisherAuthority(), is(false));
        assertThat(files.getEmbargoDate(), is(Optional.empty()));
    }

    private void mockResponses(String responseBody) throws IOException, InterruptedException {
        var doiResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(doiResponse.body()).thenReturn(IoUtils.stringFromResources(Path.of(responseBody)));
        when(httpClient.send(any(), eq(BodyHandlers.ofString()))).thenReturn(doiResponse);

        var fetchDownloadUrlResponse = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(fetchDownloadUrlResponse.body()).thenReturn(new ByteArrayInputStream(randomString().getBytes()));
        when(fetchDownloadUrlResponse.headers()).thenReturn(createDownloadUrlHeaders());
        when(httpClient.send(any(), eq(BodyHandlers.ofInputStream()))).thenReturn(fetchDownloadUrlResponse);

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                                                                               .contentType(randomString())
                                                                               .contentLength(100L).build());
    }

    private HttpHeaders createDownloadUrlHeaders() {
        return HttpHeaders.of(Map.of("Content-Type", List.of("application/pdf;charset=UTF-8"),
                                     "Content-Disposition", List.of("attachment; filename=\"someFile\"")),
                              (s, s2) -> false);
    }
}
