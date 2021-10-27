package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

public class IdentityClientImpl implements IdentityClient {

    private static final String IDENTITY_SERVICE_SECRET_NAME = "IdentityServiceSecret-";
    private static final String IDENTITY_SERVICE_SECRET_KEY = "IdentityServiceSecretKey";
    public static final String USER_SERVICE_URL = "https://api.dev.nva.aws.unit.no/identity-internal/user/";
    public static final String CUSTOMER_SERVICE_PATH = "/customer/";
    public static final String CUSTOMER_INTERNAL_PATH = "/identity-internal/customer/";
    public static final String ACCEPT = "Accept";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION = "Authorization";
    public static final String GET_USER_ERROR = "Error getting customerId from user";
    public static final String GET_CUSTOMER_ERROR = "Error getting cristinId from customer";

    private final Logger logger = LoggerFactory.getLogger(IdentityClientImpl.class);
    private final SecretsReader secretsReader;
    private final Environment environment;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    public IdentityClientImpl(SecretsReader secretsReader, Environment environment, HttpClient httpClient) {
        this.secretsReader = secretsReader;
        this.environment = environment;
        this.httpClient =  httpClient;
    }

    @JacocoGenerated
    public IdentityClientImpl() {
        this(new SecretsReader(), new Environment(), HttpClient.newHttpClient());
    }

    private String getSecret() throws ErrorReadingSecretException {
        String secretName = environment.readEnv(IDENTITY_SERVICE_SECRET_NAME);
        String secretKey = environment.readEnv(IDENTITY_SERVICE_SECRET_KEY);
        return secretsReader.fetchSecret(secretName, secretKey);
    }

    @Override
    public Optional<URI> getCustomerId(String username) {
        URI customerId = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(createGetUserUri(username))
                    .headers(ACCEPT, APPLICATION_JSON, AUTHORIZATION, getSecret())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            UserResponse userResponse = objectMapper.readValue(response.body(), UserResponse.class);
            customerId = userResponse.getCustomerId();
        } catch (ErrorReadingSecretException | IOException | InterruptedException e) {
            logger.error(GET_USER_ERROR, e);
        }
        return Optional.ofNullable(customerId);
    }

    private URI createGetUserUri(String username) {
        return URI.create(USER_SERVICE_URL + username);
    }

    @Override
    public Optional<URI> getCristinId(URI customerId) {
        URI cristinId = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(createGetCustomerUri(customerId))
                    .headers(ACCEPT, APPLICATION_JSON, AUTHORIZATION, getSecret())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            CustomerResponse customerResponse = objectMapper.readValue(response.body(), CustomerResponse.class);
            cristinId = customerResponse.getCristinId();
        } catch (ErrorReadingSecretException | IOException | InterruptedException e) {
            logger.error(GET_CUSTOMER_ERROR, e);
        }
        return Optional.ofNullable(cristinId);
    }

    private URI createGetCustomerUri(URI customerId) {
        String uri = customerId.toString().replace(CUSTOMER_SERVICE_PATH, CUSTOMER_INTERNAL_PATH);
        return URI.create(uri);
    }
}
