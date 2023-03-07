package no.unit.nva.doi;

import static no.unit.nva.doi.handlers.ReserveDoiHandler.BAD_RESPONSE_ERROR_MESSAGE;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.doi.model.ReserveDoiRequest;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.BackendClientCredentials;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class DataCiteReserveDoiClient implements ReserveDoiClient {

    public static final String DOI_REGISTRAR = "doi-registrar";
    public static final String API_HOST = "API_HOST";
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    private final Environment environment;
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;


    public DataCiteReserveDoiClient(HttpClient httpClient, SecretsManagerClient secretsManagerClient,
                                    Environment environment) {
        this.httpClient = httpClient;
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.environment = environment;
    }

    @Override
    public URI generateDoi(Publication publication) {
        return attempt(this::constructUri)
                   .map(uri -> sendRequest(uri, publication))
                   .map(this::validateResponse)
                   .map(this::convertResponseToDoi)
                   .orElseThrow();
    }

    private HttpResponse<String> validateResponse(HttpResponse<String> response) throws BadGatewayException {
         if(HttpURLConnection.HTTP_CREATED == response.statusCode()) {
            return response;
        } else {
            throw new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE);
        }
    }

    private CognitoCredentials fetchCredentials() {
        var credentials = secretsReader.fetchClassSecret(BACKEND_CLIENT_SECRET_NAME, BackendClientCredentials.class);
        var uri = UriWrapper.fromHost(BACKEND_CLIENT_AUTH_URL).getUri();
        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    private URI constructUri() {
        return UriWrapper.fromUri(environment.readEnv(API_HOST))
                   .addChild(DOI_REGISTRAR)
                   .getUri();
    }

    private HttpResponse<String> sendRequest(URI uri, Publication publication) throws IOException,
                                                                              InterruptedException {
        var request = HttpRequest.newBuilder()
                          .POST(withBody(publication))
                          .uri(uri);
        var authorizedBackendClient = getAuthorizedBackendClient();
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private AuthorizedBackendClient getAuthorizedBackendClient() {
        var credentials = fetchCredentials();
        return AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient, credentials);
    }

    private static BodyPublisher withBody(Publication publication) throws JsonProcessingException {
        var doiRequest = new ReserveDoiRequest(publication.getPublisher().getId());
        var body = JsonUtils.dtoObjectMapper.writeValueAsString(doiRequest);
        return BodyPublishers.ofString(body);
    }

    public URI convertResponseToDoi(HttpResponse<String> response) throws JsonProcessingException {
        var doiResponse = JsonUtils.dtoObjectMapper.readValue(response.body(), DoiResponse.class);
        return doiResponse.getDoi();
    }
}
