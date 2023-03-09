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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class DataCiteReserveDoiClient implements ReserveDoiClient {

    public static final String DOI_REGISTRAR = "doi-registrar";
    public static final String API_HOST = "API_HOST";
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String DATACITE_BAD_RESPONSE_ERROR_MESSAGE = "Bad response from DataCite, DataCite responded "
                                                                     + "with status code: {}";
    public static final String DATA_CITE_CONFIGURATION_ERROR = "Configuration error from DataCite: {}";
    private static final Logger logger = LoggerFactory.getLogger(DataCiteReserveDoiClient.class);
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
        logger.info("Attempting to send request");
        return attempt(() -> sendRequest(publication))
                   .map(this::validateResponse)
                   .map(this::convertResponseToDoi)
                   .orElseThrow();
    }

    private URI constructUri() {
        logger.info("constructing uri");
        return URI.create("https://" + environment.readEnv(API_HOST) + "/" + DOI_REGISTRAR);
    }

    private HttpResponse<String> sendRequest(Publication publication) throws IOException,
                                                                             InterruptedException {
        var request = HttpRequest.newBuilder()
                          .POST(withBody(publication))
                          .uri(constructUri());
        logger.info("Request to send: {}", request.build().uri());
        var authorizedBackendClient = getAuthorizedBackendClient();
        logger.info("Authorized client initialized");
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> validateResponse(HttpResponse<String> response) throws BadGatewayException {
        logger.info("Response from DataCite {}", response.body());
        logger.info("Status code from DataCite {}", response.statusCode());
        if (HttpURLConnection.HTTP_CREATED == response.statusCode()) {
            return response;
        } else {
            logErrorMessage(response);
            throw new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE);
        }
    }

    private void logErrorMessage(HttpResponse<String> response) {
        var statusCode = response.statusCode();
        if (statusCode > HttpURLConnection.HTTP_INTERNAL_ERROR) {
            logger.error(DATACITE_BAD_RESPONSE_ERROR_MESSAGE, statusCode);
        }
        if (statusCode > HttpURLConnection.HTTP_BAD_REQUEST) {
            logger.error(DATA_CITE_CONFIGURATION_ERROR, statusCode);
        }
    }

    public URI convertResponseToDoi(HttpResponse<String> response) throws JsonProcessingException {
        var doiResponse = JsonUtils.dtoObjectMapper.readValue(response.body(), DoiResponse.class);
        logger.info("Doi from response: {}", doiResponse.getDoi());
        return doiResponse.getDoi();
    }

    private static BodyPublisher withBody(Publication publication) throws JsonProcessingException {
        logger.info("Building boyd");
        var doiRequest = new ReserveDoiRequest(publication.getPublisher().getId());
        var body = JsonUtils.dtoObjectMapper.writeValueAsString(doiRequest);
        logger.info("body is ready");
        return BodyPublishers.ofString(body);
    }

    private AuthorizedBackendClient getAuthorizedBackendClient() {
        var credentials = fetchCredentials();
        return AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient, credentials);
    }

    private CognitoCredentials fetchCredentials() {
        var credentials = secretsReader.fetchClassSecret(BACKEND_CLIENT_SECRET_NAME, BackendClientCredentials.class);
        var uri = UriWrapper.fromHost(BACKEND_CLIENT_AUTH_URL).getUri();
        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }
}
