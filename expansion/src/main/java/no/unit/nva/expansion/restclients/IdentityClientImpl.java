package no.unit.nva.expansion.restclients;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.expansion.ExpansionConstants.API_HOST;
import static no.unit.nva.expansion.ExpansionConstants.API_SCHEME;
import static no.unit.nva.expansion.ExpansionConstants.CUSTOMER_INTERNAL_PATH;
import static no.unit.nva.expansion.ExpansionConstants.CUSTOMER_SERVICE_PATH;
import static no.unit.nva.expansion.ExpansionConstants.IDENTITY_SERVICE_SECRET_KEY;
import static no.unit.nva.expansion.ExpansionConstants.IDENTITY_SERVICE_SECRET_NAME;
import static no.unit.nva.expansion.ExpansionConstants.USER_INTERNAL_SERVICE_PATH;
import static nva.commons.core.attempt.Try.attempt;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import no.unit.nva.expansion.restclients.responses.CustomerResponse;
import no.unit.nva.expansion.restclients.responses.UserResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityClientImpl implements IdentityClient {

    public static final String ERROR_READING_SECRETS_ERROR =
        "Could not read secrets for internal communication with identity service";
    private static final String GET_USER_ERROR = "Error getting customerId from user";
    private static final String GET_CUSTOMER_ERROR = "Error getting cristinId from customer";
    public static final String RESPONSE_STATUS_BODY = "Response status=%s, body=%s";
    private final Logger logger = LoggerFactory.getLogger(IdentityClientImpl.class);
    private final HttpClient httpClient;
    private final String identityServiceSecret;

    public IdentityClientImpl(SecretsReader secretsReader, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.identityServiceSecret = attempt(() -> fetchSecret(secretsReader))
            .orElseThrow(fail -> logAndFail(fail.getException()));
    }

    @JacocoGenerated
    public IdentityClientImpl() {
        this(new SecretsReader(), HttpClient.newHttpClient());
    }

    @Override
    public Optional<URI> getCustomerId(String username) {
        URI customerId = null;
        try {
            HttpRequest request = createGetUserHttpRequest(createGetUserInternalUri(username));
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                customerId = UserResponse.fromJson(response.body()).getCustomerId();
            } else {
                logWarning(response);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(GET_USER_ERROR, e);
        }
        return Optional.ofNullable(customerId);
    }

    private void logWarning(HttpResponse<String> response) {
        logger.warn(String.format(RESPONSE_STATUS_BODY, response.statusCode(), response.body()));
    }

    @Override
    public Optional<URI> getCristinId(URI customerId) {
        URI cristinId = null;
        try {
            HttpRequest request = createGetCustomerHttpRequest(customerId);
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                cristinId = CustomerResponse.fromJson(response.body()).getCristinId();
            } else {
                logWarning(response);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(GET_CUSTOMER_ERROR, e);
        }
        return Optional.ofNullable(cristinId);
    }

    private RuntimeException logAndFail(Exception exception) {
        logger.error(ERROR_READING_SECRETS_ERROR);
        return new RuntimeException(exception);
    }

    private String fetchSecret(SecretsReader secretsReader) throws ErrorReadingSecretException {
        return secretsReader.fetchSecret(IDENTITY_SERVICE_SECRET_NAME, IDENTITY_SERVICE_SECRET_KEY);
    }

    private HttpRequest createGetUserHttpRequest(URI getUserUri) {
        return HttpRequest.newBuilder()
            .uri(getUserUri)
            .headers(ACCEPT, JSON_UTF_8.toString(), AUTHORIZATION, identityServiceSecret)
            .GET()
            .build();
    }

    private URI createGetUserInternalUri(String username) {
        return new UriWrapper(API_SCHEME, API_HOST)
            .addChild(USER_INTERNAL_SERVICE_PATH)
            .addChild(username)
            .getUri();
    }

    private HttpRequest createGetCustomerHttpRequest(URI customerId) {
        return HttpRequest.newBuilder()
            .uri(createGetCustomerInternalUri(customerId))
            .headers(ACCEPT, JSON_UTF_8.toString(), AUTHORIZATION, identityServiceSecret)
            .GET()
            .build();
    }

    private URI createGetCustomerInternalUri(URI customerId) {
        String uri = customerId.toString().replace(CUSTOMER_SERVICE_PATH, CUSTOMER_INTERNAL_PATH);
        return URI.create(uri);
    }
}
