package no.unit.nva.doi;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
public class DataCiteDoiClientTest {

    public static final String ACCESS_TOKEN_RESPONSE_BODY = "{ \"access_token\" : \"Bearer token\"}";
    private final Environment environment = mock(Environment.class);
    private FakeSecretsManagerClient secretsManagerClient;
    private DataCiteDoiClient dataCiteDoiClient;

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("someSecret", credentials.toString());
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv("API_HOST")).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl());
        dataCiteDoiClient = new DataCiteDoiClient(WiremockHttpClient.create(), secretsManagerClient,
                                                  wireMockRuntimeInfo.getHttpsBaseUrl());
    }

    @Test
    void shouldThrowExceptionWhenDoiRegistrarEndpointIsNotReachable() {
        var publication = PublicationGenerator.randomPublication();
        assertThrows(RuntimeException.class, () -> dataCiteDoiClient.createFindableDoi(publication));
    }

    @Test
    void shouldReturnDoiResponseWhenPostIsSuccessful(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var doi = RandomDataGenerator.randomDoi();
        var httpClient = new FakeHttpClient<>(tokenResponse(), findableDoiResponse(doi));
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, wireMockRuntimeInfo.getHttpsBaseUrl());
        var actualDoi = doiClient.createFindableDoi(publication);
        assertThat(actualDoi, is(equalTo(doi)));
    }

    @Test
    void shouldThrowExceptionWhenBadResponseFromDoiClient(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = new FakeHttpClient<>(tokenResponse(), deleteDoiBadResponse());
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, wireMockRuntimeInfo.getHttpsBaseUrl());
        assertThrows(RuntimeException.class, () -> doiClient.deleteDraftDoi(publication));
    }

    @Test
    void shouldThrowExceptionWhenBadMethodFromDoiClient(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var publication = PublicationGenerator.randomPublication();
        var httpClient = new FakeHttpClient<>(tokenResponse(), deleteDoiBadMethodResponse());
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient, wireMockRuntimeInfo.getHttpsBaseUrl());
        assertThrows(RuntimeException.class, () -> doiClient.deleteDraftDoi(publication));
    }

    @Test
    void shouldReturnStatusCodeAcceptedOnSuccessfulDeleteDoi() {
        var publication = PublicationGenerator.randomPublication();
        var doiClient = mock(DataCiteDoiClient.class);
        doiClient.deleteDraftDoi(publication);
        verify(doiClient, times(1)).deleteDraftDoi(publication);
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
