package no.unit.nva.doi;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
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
import no.unit.nva.testutils.JwtTestToken;
import no.unit.nva.testutils.RandomDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DataCiteDoiClientTest {

    private static final String ACCESS_TOKEN_RESPONSE_BODY =
        """
           {
           "access_token" : "%s"
           }
        """.formatted(JwtTestToken.randomToken());
    private static final String HOST = "localhost";

    private FakeSecretsManagerClient secretsManagerClient;
    private DataCiteDoiClient dataCiteDoiClient;

    @BeforeEach
    void setup() {
        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("someSecret", credentials.toString());
        dataCiteDoiClient = new DataCiteDoiClient(HttpClient.newHttpClient(), secretsManagerClient, HOST);
    }

    @Test
    void shouldThrowExceptionWhenDoiRegistrarEndpointIsNotReachable() {
        var publication = PublicationGenerator.randomPublication();
        var customer = publication.getPublisher().getId();
        Executable executable = () -> dataCiteDoiClient.createFindableDoi(customer, publication);
        assertThrows(RuntimeException.class, executable);
    }

    @Test
    void shouldReturnDoiResponseWhenPostIsSuccessful() throws JsonProcessingException {
        var doi = RandomDataGenerator.randomDoi();
        var publication = PublicationGenerator.randomPublication();
        var doiClient = getDataCiteDoiClient(doi);
        var customer = publication.getPublisher().getId();
        var actualDoi = doiClient.createFindableDoi(customer, publication);
        assertThat(actualDoi, is(equalTo(doi)));
    }

    @Test
    void shouldThrowExceptionWhenBadResponseFromDoiClient() {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = new FakeHttpClient<>(tokenResponse(), deleteDoiBadResponse());
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, HOST);
        var requestingCustomer = publication.getPublisher().getId();
        Executable executable = () -> doiClient.deleteDraftDoi(requestingCustomer, publication);
        assertThrows(RuntimeException.class, executable);
    }

    @Test
    void shouldThrowExceptionWhenBadMethodFromDoiClient() {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = new FakeHttpClient<>(tokenResponse(), deleteDoiBadMethodResponse());
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, HOST);
        var requestingCustomer = publication.getPublisher().getId();
        Executable executable = () -> doiClient.deleteDraftDoi(requestingCustomer, publication);
        assertThrows(RuntimeException.class, executable);
    }

    @Test
    void shouldReturnStatusCodeAcceptedOnSuccessfulDeleteDoi() throws IOException, InterruptedException {
        var publication = PublicationGenerator.randomPublication();
        var doiClient = dataciteClientReturning(HTTP_ACCEPTED);
        var requestingCustomer = publication.getPublisher().getId();
        doiClient.deleteDraftDoi(requestingCustomer, publication);
        verify(doiClient, times(1)).deleteDraftDoi(requestingCustomer, publication);
    }

    @Test
    void shouldThrowWhenFailingValidateResponseOnDeleteDoi() throws IOException, InterruptedException {
        var publication = PublicationGenerator.randomPublication();
        var doiClient = dataciteClientReturning(HTTP_CONFLICT);
        var requestingCustomer = publication.getPublisher().getId();
        Executable executable = () -> doiClient.deleteDraftDoi(requestingCustomer, publication);
        assertThrows(RuntimeException.class, executable);
    }

    private DataCiteDoiClient dataciteClientReturning(int httpConflict)
        throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any())).thenReturn(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK))
            .thenReturn(FakeHttpResponse.create(null, httpConflict));
        return spy(new DataCiteDoiClient(httpClient, secretsManagerClient, HOST));
    }

    private DataCiteDoiClient getDataCiteDoiClient(URI doi) throws JsonProcessingException {
        var httpClient = new FakeHttpClient<>(tokenResponse(), findableDoiResponse(doi));
        return new DataCiteDoiClient(httpClient, secretsManagerClient, HOST);
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
