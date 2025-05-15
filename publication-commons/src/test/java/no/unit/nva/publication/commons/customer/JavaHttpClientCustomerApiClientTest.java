package no.unit.nva.publication.commons.customer;

import static no.unit.nva.model.testing.RandomUtils.randomBackendUri;
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
import java.net.http.HttpResponse;
import no.unit.nva.auth.AuthorizedBackendClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JavaHttpClientCustomerApiClientTest {

    private AuthorizedBackendClient authorizedBackendClient;

    @BeforeEach
    void setup() throws IOException, InterruptedException {
        authorizedBackendClient = mock(AuthorizedBackendClient.class);

        doReturn(okCustomerResponse())
            .when(authorizedBackendClient)
            .send(argThat(request -> request.build().uri().getPath().startsWith("/customer/")), any());
    }

    @Test
    void shouldReturnCustomerIfEverythingIsOk() {
        var customerUri = randomBackendUri("customer");

        var customerApiClient = getJavaHttpClientCustomerApiClient();

        var customer = customerApiClient.fetch(customerUri);

        assertThat(customer.getPublicationWorkflow(), is(equalTo("myWorkflow")));
        assertThat(customer.getAllowFileUploadForTypes(), containsInAnyOrder("someType"));
        assertThat(customer.getRightsRetentionStrategy().getType(), is(equalTo("NullRightsRetentionStrategy")));
        assertThat(customer.getRightsRetentionStrategy().getId(), is(equalTo("https://example.org/1")));
    }

    private JavaHttpClientCustomerApiClient getJavaHttpClientCustomerApiClient() {
        return new JavaHttpClientCustomerApiClient(authorizedBackendClient);
    }

    @Test
    void shouldThrowExceptionIfNotSuccessFromCustomerApi() throws IOException, InterruptedException {
        var customerUri = randomBackendUri("customer");

        doReturn(failedCustomerResponse())
            .when(authorizedBackendClient)
            .send(argThat(request -> request.build().uri().getPath().startsWith("/customer/")), any());

        var customerApiClient = getJavaHttpClientCustomerApiClient();

        assertThrows(CustomerNotAvailableException.class, () -> customerApiClient.fetch(customerUri));
    }

    @Test
    void shouldThrowExceptionIfDeserializationOfResponseFails() throws IOException, InterruptedException {
        var customerUri = randomBackendUri("customer");

        doReturn(customerResponseWithInvalidJsonFormat())
            .when(authorizedBackendClient)
            .send(argThat(request -> request.build().uri().getPath().startsWith("/customer/")), any());

        var customerApiClient = getJavaHttpClientCustomerApiClient();

        assertThrows(CustomerNotAvailableException.class, () -> customerApiClient.fetch(customerUri));
    }

    @Test
    void shouldThrowExceptionIfIoFails() throws IOException, InterruptedException {
        var customerUri = randomBackendUri("customer");

        doThrow(new ConnectException())
            .when(authorizedBackendClient)
            .send(argThat(request -> request.build().uri().getPath().startsWith("/customer/")), any());

        var customerApiClient = getJavaHttpClientCustomerApiClient();

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
}
