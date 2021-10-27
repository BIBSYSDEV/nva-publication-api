package no.unit.nva.expansion.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.expansion.Constants;
import no.unit.nva.expansion.IdentityClient;
import no.unit.nva.expansion.model.CustomerResponse;
import no.unit.nva.expansion.model.UserResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.paths.UriWrapper;
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

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_OK;

public class IdentityClientImpl implements IdentityClient {

    private static final String GET_USER_ERROR = "Error getting customerId from user";
    private static final String GET_CUSTOMER_ERROR = "Error getting cristinId from customer";

    private final Logger logger = LoggerFactory.getLogger(IdentityClientImpl.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private final String identityServiceSecret;

    public IdentityClientImpl(SecretsReader secretsReader, HttpClient httpClient) throws ErrorReadingSecretException {
        this.httpClient =  httpClient;
        this.identityServiceSecret = secretsReader.fetchSecret(
                Constants.IDENTITY_SERVICE_SECRET_NAME, Constants.IDENTITY_SERVICE_SECRET_KEY);
    }

    @JacocoGenerated
    public IdentityClientImpl() throws ErrorReadingSecretException {
        this(new SecretsReader(), HttpClient.newHttpClient());
    }


    @Override
    public Optional<URI> getCustomerId(String username) {
        URI customerId = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(createGetUserUri(username))
                    .headers(ACCEPT, JSON_UTF_8.toString(), AUTHORIZATION, identityServiceSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                UserResponse userResponse = objectMapper.readValue(response.body(), UserResponse.class);
                customerId = userResponse.getCustomerId();
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(GET_USER_ERROR, e);
        }
        return Optional.ofNullable(customerId);
    }

    private URI createGetUserUri(String username) {
        return new UriWrapper(Constants.API_SCHEME, Constants.API_HOST)
                .addChild(Constants.USER_INTERNAL_SERVICE_PATH)
                .addChild(username)
                .getUri();
    }

    @Override
    public Optional<URI> getCristinId(URI customerId) {
        URI cristinId = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(createGetCustomerUri(customerId))
                    .headers(ACCEPT, JSON_UTF_8.toString(), AUTHORIZATION, identityServiceSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                CustomerResponse customerResponse = objectMapper.readValue(response.body(), CustomerResponse.class);
                cristinId = customerResponse.getCristinId();
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(GET_CUSTOMER_ERROR, e);
        }
        return Optional.ofNullable(cristinId);
    }

    private URI createGetCustomerUri(URI customerId) {
        String uri = customerId.toString().replace(Constants.CUSTOMER_SERVICE_PATH, Constants.CUSTOMER_INTERNAL_PATH);
        return URI.create(uri);
    }
}
