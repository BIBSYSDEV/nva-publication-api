package no.unit.nva.publication.commons.customer;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.commons.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaHttpClientCustomerApiClient implements CustomerApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaHttpClientCustomerApiClient.class);

    private final HttpClient httpClient;
    private final CognitoCredentials cognitoCredentials;

    public JavaHttpClientCustomerApiClient(final HttpClient httpClient,
                                           final CognitoCredentials cognitoCredentials) {
        this.httpClient = httpClient;
        this.cognitoCredentials = cognitoCredentials;
    }

    @Override
    public Customer fetch(URI customerId) {
        var authorizedBackendClient = AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient,
                                                                                            cognitoCredentials);
        var requestBuilder = createGetRequest(customerId);
        var response = executeRequest(customerId, authorizedBackendClient, requestBuilder);

        return deserializeResponseOrThrowException(customerId, response);
    }

    private static Customer deserializeResponseOrThrowException(URI customerId, HttpResponse<String> response) {
        if (response.statusCode() == HTTP_OK) {
            return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response.body(), Customer.class))
                       .orElseThrow(fail -> new CustomerNotAvailableException(customerId, fail.getException()));
        } else {
            LOGGER.warn("Got unexpected response code from upstream {}: {}", customerId, response.statusCode());
            throw new CustomerNotAvailableException(customerId);
        }
    }

    private static HttpResponse<String> executeRequest(URI customerId, AuthorizedBackendClient authorizedBackendClient,
                                                    Builder requestBuilder) {
        return attempt(() -> authorizedBackendClient.send(requestBuilder, BodyHandlers.ofString()))
                   .orElseThrow(fail -> new CustomerNotAvailableException(customerId, fail.getException()));
    }

    private static Builder createGetRequest(final URI customerId) {
        return HttpRequest.newBuilder(customerId)
                   .header("Accept", "application/json")
                   .GET();
    }
}
