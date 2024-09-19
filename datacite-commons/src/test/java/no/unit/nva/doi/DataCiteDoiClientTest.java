package no.unit.nva.doi;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.JwtTestToken;
import no.unit.nva.testutils.RandomDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataCiteDoiClientTest {

    public static final String ACCESS_TOKEN_RESPONSE_BODY =
        """
           {
           "access_token" : "%s"
           }
        """.formatted(JwtTestToken.randomToken());
    private FakeSecretsManagerClient secretsManagerClient;
    private DataCiteDoiClient dataCiteDoiClient;

    @BeforeEach
    void setup() {
        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("someSecret", credentials.toString());
        dataCiteDoiClient = new DataCiteDoiClient(WiremockHttpClient.create(), secretsManagerClient,
                                                  "http://localhost:68000");
    }

    @Test
    void shouldThrowExceptionWhenDoiRegistrarEndpointIsNotReachable() {
        var publication = PublicationGenerator.randomPublication();
        assertThrows(RuntimeException.class, () -> dataCiteDoiClient.createFindableDoi(publication));
    }

    @Test
    void shouldReturnDoiResponseWhenPostIsSuccessful()
        throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var doi = RandomDataGenerator.randomDoi();
        var httpClient = new FakeHttpClient<>(tokenResponse(), findableDoiResponse(doi));
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, randomString());
        var actualDoi = doiClient.createFindableDoi(publication);
        assertThat(actualDoi, is(equalTo(doi)));
    }

    @Test
    void shouldThrowExceptionWhenBadResponseFromDoiClient() {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = new FakeHttpClient<>(tokenResponse(), deleteDoiBadResponse());
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, randomString());
        assertThrows(RuntimeException.class, () -> doiClient.deleteDraftDoi(publication));
    }

    @Test
    void shouldThrowExceptionWhenBadMethodFromDoiClient() {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = new FakeHttpClient<>(tokenResponse(), deleteDoiBadMethodResponse());
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, randomString());
        assertThrows(RuntimeException.class, () -> doiClient.deleteDraftDoi(publication));
    }

    @Test
    void shouldReturnStatusCodeAcceptedOnSuccessfulDeleteDoi() throws IOException, InterruptedException {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any())).thenReturn(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK))
            .thenReturn(FakeHttpResponse.create(null, HTTP_ACCEPTED));

        var doiClient = spy(new DataCiteDoiClient(httpClient, secretsManagerClient,"url"));
        doiClient.deleteDraftDoi(publication);
        verify(doiClient, times(1)).deleteDraftDoi(publication);
    }

    @Test
    void shouldThrowWhenFailingValidateResponseOnDeleteDoi() throws IOException, InterruptedException {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any())).thenReturn(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK))
            .thenReturn(FakeHttpResponse.create(null, HTTP_CONFLICT));
        var doiClient = spy(new DataCiteDoiClient(httpClient, secretsManagerClient,"url"));

        assertThrows(RuntimeException.class, () -> doiClient.deleteDraftDoi(publication));
    }

    private static String createResponse(String expectedDoiPrefix) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.writeValueAsString(new DoiResponse(URI.create(expectedDoiPrefix)));
    }

    private static FakeHttpResponse<String> tokenResponse() {
        return FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK);
    }

    private FakeHttpResponse<String> deleteDoiBadResponse() {
        return FakeHttpResponse.create(null, HTTP_BAD_GATEWAY);
    }

    private FakeHttpResponse<String> deleteDoiBadMethodResponse() {
        return FakeHttpResponse.create(null, HTTP_BAD_METHOD);
    }

    private FakeHttpResponse<String> findableDoiResponse(URI expectedDoi) throws JsonProcessingException {
        return FakeHttpResponse.create(createResponse(expectedDoi.toString()), HTTP_CREATED);
    }
}
