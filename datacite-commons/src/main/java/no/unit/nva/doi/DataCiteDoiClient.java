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
import java.util.Optional;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.DoiRequest;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.doi.model.ReserveDoiRequest;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.BackendClientCredentials;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class DataCiteDoiClient implements DoiClient {

    public static final String DOI_REGISTRAR = "doi-registrar";
    public static final String DRAFT = "draft";
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String DATACITE_BAD_RESPONSE_ERROR_MESSAGE = "Bad response from DataCite, DataCite responded "
                                                                     + "with status code: {}";
    public static final String DATA_CITE_CONFIGURATION_ERROR = "Configuration error from DataCite: {}";
    public static final String FINDABLE = "findable";
    public static final String PUBLICATION = "publication";
    public static final String DELETE = "delete";
    private static final Logger logger = LoggerFactory.getLogger(DataCiteDoiClient.class);
    private final String apiHost;
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;

    public DataCiteDoiClient(HttpClient httpClient, SecretsManagerClient secretsManagerClient,
                             String apiHost) {
        this.httpClient = httpClient;
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.apiHost = apiHost;
    }

    @Override
    public URI generateDraftDoi(Publication publication) {
        return attempt(() -> sendDraftDoiRequest(publication))
                   .map(this::validateResponse)
                   .map(this::convertResponseToDoi)
                   .orElseThrow();
    }

    @Override
    public URI createFindableDoi(Publication publication) {
        return attempt(() -> sendFindableDoiRequest(publication))
                   .map(this::validateResponse)
                   .map(this::convertResponseToDoi)
                   .orElseThrow();
    }

    @Override
    public void deleteDraftDoi(Publication publication) {
        attempt(() -> sendDeleteDraftDoiRequest(publication))
            .map(this::validateResponseStatusCode)
            .orElseThrow();
    }

    private static BodyPublisher withDraftDoiRequestBody(Publication publication) throws JsonProcessingException {
        var doiRequest = new ReserveDoiRequest(publication.getPublisher().getId());
        var body = JsonUtils.dtoObjectMapper.writeValueAsString(doiRequest);
        return BodyPublishers.ofString(body);
    }

    private HttpResponse<String> validateResponseStatusCode(HttpResponse<String> response) throws BadGatewayException {
        if (HttpURLConnection.HTTP_ACCEPTED == response.statusCode()) {
            return response;
        } else {
            logErrorMessage(response);
            throw new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE);
        }
    }

    private AuthorizedBackendClient getAuthorizedBackendClient(CognitoCredentials cognitoCredentials) {
        return AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient, cognitoCredentials);
    }

    private CognitoCredentials fetchCredentials() {
        var credentials = secretsReader.fetchClassSecret(BACKEND_CLIENT_SECRET_NAME, BackendClientCredentials.class);
        var uri = UriWrapper.fromHost(BACKEND_CLIENT_AUTH_URL).getUri();
        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    private URI convertResponseToDoi(HttpResponse<String> response) throws JsonProcessingException {
        var doiResponse = JsonUtils.dtoObjectMapper.readValue(response.body(), DoiResponse.class);
        return doiResponse.getDoi();
    }

    private HttpResponse<String> sendFindableDoiRequest(Publication publication)
        throws IOException, InterruptedException, BadRequestException {
        var request = HttpRequest.newBuilder()
                          .POST(withDoiRequestBody(publication))
                          .uri(constructFindableDoiUri());
        var authorizedBackendClient = getAuthorizedBackendClient(fetchCredentials());
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDraftDoiRequest(Publication publication) throws IOException,
                                                                                     InterruptedException {
        var request = HttpRequest.newBuilder()
                          .POST(withDraftDoiRequestBody(publication))
                          .uri(constructDraftDoiUri());
        var authorizedBackendClient = getAuthorizedBackendClient(fetchCredentials());
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDeleteDraftDoiRequest(Publication publication)
        throws BadRequestException, IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                          .POST(withDoiRequestBody(publication))
                          .uri(constructDeleteDraftDoiUri());
        var authorizedBackendClient = getAuthorizedBackendClient(fetchCredentials());
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI constructFindableDoiUri() {
        return UriWrapper.fromHost(apiHost)
                   .addChild(DOI_REGISTRAR)
                   .addChild(FINDABLE)
                   .getUri();
    }

    private URI constructDeleteDraftDoiUri() {
        return UriWrapper.fromHost(apiHost)
                   .addChild(DOI_REGISTRAR)
                   .addChild(DELETE)
                   .getUri();
    }

    private BodyPublisher withDoiRequestBody(Publication publication) throws BadRequestException {
        var doiRequest =
            new DoiRequest.Builder()
                .withDoi(publication.getDoi())
                .withCustomerId(Optional.ofNullable(publication.getPublisher()).map(Organization::getId).orElse(null))
                .withPublicationId(inferPublicationId(publication))
                .build();
        logger.info("DoiRequest: {}", doiRequest.toJsonString());
        return BodyPublishers.ofString(doiRequest.toJsonString());
    }

    private URI inferPublicationId(Publication publication) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(PUBLICATION)
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    private URI constructDraftDoiUri() {
        return UriWrapper.fromHost(apiHost)
                   .addChild(DOI_REGISTRAR)
                   .addChild(DRAFT)
                   .getUri();
    }

    private HttpResponse<String> validateResponse(HttpResponse<String> response) throws BadGatewayException {
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
}
