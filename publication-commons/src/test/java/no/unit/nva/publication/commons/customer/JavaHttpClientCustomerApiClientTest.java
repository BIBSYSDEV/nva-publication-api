package no.unit.nva.publication.commons.customer;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import no.unit.nva.auth.CognitoCredentials;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JavaHttpClientCustomerApiClientTest {

    private HttpClient httpClient;

    @BeforeEach
    void setup() throws IOException, InterruptedException {
        httpClient = mock(HttpClient.class);

        doReturn(okTokenResponse())
            .when(httpClient)
            .send(argThat(request -> request.uri().getPath().equals("/oauth2/token")), any());
    }

    public static final URI BACKEND_CLIENT_AUTH_URL = URI.create(
        new Environment().readEnv("BACKEND_CLIENT_AUTH_URL"));

    @Test
    void shouldReturnCustomerIfEverythingIsOk() throws InterruptedException, IOException {
        var customerUri = randomUri();

        var customerApiClient = getJavaHttpClientCustomerApiClient(httpClient);
        doReturn(okCustomerResponse())
            .when(httpClient)
            .send(argThat(request -> request.uri().getPath().equals(customerUri.getPath())), any());

        var customer = customerApiClient.fetch(customerUri);

        assertThat(customer.getPublicationWorkflow(), is(equalTo("myWorkflow")));
        assertThat(customer.getAllowFileUploadForTypes(), containsInAnyOrder("someType"));
        assertThat(customer.getRightsRetentionStrategy().getType(), is(equalTo("NullRightsRetentionStrategy")));
        assertThat(customer.getRightsRetentionStrategy().getId(), is(equalTo("https://example.org/1")));
    }

    private static JavaHttpClientCustomerApiClient getJavaHttpClientCustomerApiClient(HttpClient httpClient) {
        var cognitoCredentials = new CognitoCredentials(() -> "clientId",
                                                        () -> "clientSecret",
                                                        BACKEND_CLIENT_AUTH_URL);
        var customerApiClient = new JavaHttpClientCustomerApiClient(httpClient, cognitoCredentials);
        return customerApiClient;
    }

    @Test
    void shouldThrowExceptionIfNotSuccessFromCustomerApi() throws IOException, InterruptedException {
        var customerUri = randomUri();
        doReturn(failedCustomerResponse())
            .when(httpClient)
            .send(argThat(request -> request.uri().getPath().equals(customerUri.getPath())), any());

        var customerApiClient = getJavaHttpClientCustomerApiClient(httpClient);

        assertThrows(CustomerNotAvailableException.class, () -> customerApiClient.fetch(customerUri));
    }

    @Test
    void shouldThrowExceptionIfDeserializationOfResponseFails() throws IOException, InterruptedException {
        var customerUri = randomUri();
        doReturn(customerResponseWithInvalidJsonFormat())
            .when(httpClient)
            .send(argThat(request -> request.uri().getPath().equals(customerUri.getPath())), any());

        var customerApiClient = getJavaHttpClientCustomerApiClient(httpClient);

        assertThrows(CustomerNotAvailableException.class, () -> customerApiClient.fetch(customerUri));
    }

    @Test
    void shouldThrowExceptionIfIoFails() throws IOException, InterruptedException {
        var customerUri = randomUri();
        doThrow(new ConnectException())
            .when(httpClient)
            .send(argThat(request -> request.uri().getPath().equals(customerUri.getPath())), any());

        var customerApiClient = getJavaHttpClientCustomerApiClient(httpClient);

        assertThrows(CustomerNotAvailableException.class, () -> customerApiClient.fetch(customerUri));
    }

    private HttpResponse<String> customerResponseWithInvalidJsonFormat() {
        var httpResponse = mock(HttpResponse.class);

        doReturn(200).when(httpResponse).statusCode();
        var response = """
            {
                "allowFileUploadForTypes": {},
                "publicationWorkflow": "myWorkflow"
            }
            """;
        doReturn(response).when(httpResponse).body();

        //noinspection unchecked
        return httpResponse;
    }

    private HttpResponse<String> okCustomerResponse() {
        var httpResponse = mock(HttpResponse.class);

        doReturn(200).when(httpResponse).statusCode();
        var response = """
            {
                "allowFileUploadForTypes": ["someType"],
                "publicationWorkflow": "myWorkflow",
                "rightsRetentionStrategy": {
                    "type": "NullRightsRetentionStrategy",
                     "id": "https://example.org/1"
                    }
            }
            """;
        doReturn(response).when(httpResponse).body();

        //noinspection unchecked
        return httpResponse;
    }

    private HttpResponse<String> failedCustomerResponse() {
        var httpResponse = mock(HttpResponse.class);

        doReturn(404).when(httpResponse).statusCode();

        //noinspection unchecked
        return httpResponse;
    }

    private HttpResponse<String> okTokenResponse() {
        var httpResponse = mock(HttpResponse.class);

        doReturn(200).when(httpResponse).statusCode();
        var response = """
            {
                "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
            }
            """;
        doReturn(response).when(httpResponse).body();

        //noinspection unchecked
        return httpResponse;
    }
}
