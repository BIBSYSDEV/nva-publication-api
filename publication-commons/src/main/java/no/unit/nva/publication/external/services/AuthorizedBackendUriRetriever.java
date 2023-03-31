package no.unit.nva.publication.external.services;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.net.ssl.HttpsURLConnection;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.publication.model.BackendClientCredentials;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class AuthorizedBackendUriRetriever implements RawContentRetriever {

    public static final String FAILED_TO_RETRIEVE_URI = "Failed to retrieve uri {}";
    public static final String API_RESPONDED_WITH_ERROR_CODE = "Api responded with: ";
    public static final String ACCEPT = "Accept";
    private static final Logger logger = LoggerFactory.getLogger(AuthorizedBackendUriRetriever.class);
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;

    private final String backendClientAuthUrl;

    private final String backendClientSecretName;

    public AuthorizedBackendUriRetriever(HttpClient httpClient,
                                         SecretsReader secretsReader,
                                         String backendClientAuthUrl,
                                         String backendClientSecretName) {
        this.httpClient = httpClient;
        this.secretsReader = secretsReader;
        this.backendClientAuthUrl = backendClientAuthUrl;
        this.backendClientSecretName = backendClientSecretName;
    }

    @JacocoGenerated
    public AuthorizedBackendUriRetriever(String backendClientAuthUrl, String backendClientSecretName) {
        this(HttpClient.newHttpClient(),
             new SecretsReader(),
             backendClientAuthUrl,
             backendClientSecretName);
    }

    @Override
    public <T> Optional<T> getDto(URI uri, Class<T> valueType) {
        var request = HttpRequest.newBuilder(uri).GET();
        var responseBodyHandler = BodyHandlers.ofString(StandardCharsets.UTF_8);
        return attempt(this::getAuthorizedBackendClient)
                .map(backendClient -> backendClient.send(request, responseBodyHandler))
                .map(response -> dtoObjectMapper.readValue(response.body(), valueType))
                .toOptional();
    }

    @Override
    public Optional<String> getRawContent(URI uri, String mediaType) {
        return attempt(this::getAuthorizedBackendClient)
                   .map(authorizedBackendClient -> getHttpResponse(authorizedBackendClient, uri, mediaType))
                   .map(this::getRawContentFromHttpResponse)
                   .toOptional();
    }

    private URI getCognitoTokenUrl() {
        return UriWrapper.fromHost(backendClientAuthUrl).getUri();
    }

    private String getRawContentFromHttpResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpsURLConnection.HTTP_OK) {
            logger.error(FAILED_TO_RETRIEVE_URI, response);
            throw new RuntimeException(API_RESPONDED_WITH_ERROR_CODE + response.statusCode());
        }
        return response.body();
    }

    private CognitoCredentials fetchCredentials() {
        var credentials
            = secretsReader.fetchClassSecret(backendClientSecretName, BackendClientCredentials.class);
        var uri = getCognitoTokenUrl();

        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    private AuthorizedBackendClient getAuthorizedBackendClient() {
        return AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient,
                                                                     fetchCredentials());
    }

    private HttpResponse<String> getHttpResponse(AuthorizedBackendClient backendClient,
                                                 URI customerId,
                                                 String mediaType) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(customerId).headers(ACCEPT, mediaType).GET();
        return backendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
