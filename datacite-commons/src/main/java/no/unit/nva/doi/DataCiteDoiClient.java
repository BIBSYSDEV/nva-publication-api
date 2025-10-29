package no.unit.nva.doi;

import static no.unit.nva.doi.handlers.ReserveDoiHandler.BAD_RESPONSE_ERROR_MESSAGE;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;
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
import no.unit.nva.doi.model.DoiRequest;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.doi.model.ReserveDoiRequest;
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
    public static final String NOT_ALLOWED_TO_DELETE_FINDABLE_DOI = "Not allowed to delete findable doi {}";
    private static final Logger logger = LoggerFactory.getLogger(DataCiteDoiClient.class);
    public static final String CUSTOMER_ID = "customerId";
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
    public URI generateDraftDoi(URI requestingCustomer) {
        return attempt(() -> sendDraftDoiRequest(requestingCustomer))
                   .map(this::validateResponse)
                   .map(this::convertResponseToDoi)
                   .orElseThrow();
    }

    @Override
    public URI createFindableDoi(URI requestingCustomer, Publication publication) {
        return attempt(() -> sendFindableDoiRequest(requestingCustomer, publication))
                   .map(this::validateResponse)
                   .map(this::convertResponseToDoi)
                   .orElseThrow();
    }

    @Override
    public void deleteDraftDoi(URI requestingCustomer, Publication publication) {
        attempt(() -> sendDeleteDraftDoiRequest(publication))
            .map(this::validateDeleteResponse)
            .orElseThrow();
    }

    private static BodyPublisher withDraftDoiRequestBody(URI requestingCustomer) throws JsonProcessingException {
        var doiRequest = new ReserveDoiRequest(requestingCustomer);
        var body = JsonUtils.dtoObjectMapper.writeValueAsString(doiRequest);
        return BodyPublishers.ofString(body);
    }

    private static String getDoiSuffix(URI doi) {
        return doi.getPath().split("/")[2];
    }

    private static String getDoiPrefix(URI doi) {
        return doi.getPath().split("/")[1];
    }

    private HttpResponse<String> validateDeleteResponse(HttpResponse<String> response) throws BadGatewayException {
        if (HttpURLConnection.HTTP_ACCEPTED == response.statusCode()) {
            return response;
        }
        if (HttpURLConnection.HTTP_BAD_METHOD == response.statusCode()) {
            logger.error(NOT_ALLOWED_TO_DELETE_FINDABLE_DOI, response.statusCode());
        } else {
            logErrorMessage(response);
        }
        throw new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE);
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

    private HttpResponse<String> sendFindableDoiRequest(URI requestingCustomer, Publication publication)
        throws IOException, InterruptedException, BadRequestException {
        var request = HttpRequest.newBuilder()
                          .POST(withDoiRequestBody(requestingCustomer, publication))
                          .uri(constructFindableDoiUri());
        var authorizedBackendClient = getAuthorizedBackendClient(fetchCredentials());
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDraftDoiRequest(URI requestingCustomer) throws IOException,
                                                                                    InterruptedException {
        var request = HttpRequest.newBuilder()
                          .POST(withDraftDoiRequestBody(requestingCustomer))
                          .uri(constructDraftDoiUri());
        var authorizedBackendClient = getAuthorizedBackendClient(fetchCredentials());
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDeleteDraftDoiRequest(Publication publication)
        throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                          .DELETE()
                          .uri(constructDeleteDraftDoiUri(publication));
        var authorizedBackendClient = getAuthorizedBackendClient(fetchCredentials());
        return authorizedBackendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI constructFindableDoiUri() {
        return UriWrapper.fromHost(apiHost)
                   .addChild(DOI_REGISTRAR)
                   .addChild(FINDABLE)
                   .getUri();
    }

    private URI constructDeleteDraftDoiUri(Publication publication) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(DOI_REGISTRAR)
                   .addChild(DRAFT)
                   .addChild(getDoiPrefix(publication.getDoi()))
                   .addChild(getDoiSuffix(publication.getDoi()))
                   .addQueryParameter(CUSTOMER_ID, encodeCustomer(publication))
                   .getUri();
    }

    private static String encodeCustomer(Publication publication) {
        return urlEncode(String.valueOf(publication.getPublisher().getId()));
    }

    private BodyPublisher withDoiRequestBody(URI requestingCustomer, Publication publication) throws BadRequestException {
        var doiRequest =
            new DoiRequest.Builder()
                .withDoi(publication.getDoi())
                .withCustomerId(requestingCustomer)
                .withPublicationId(inferPublicationId(publication))
                .build();
        var jsonString = doiRequest.toJsonString();
        logger.info("DoiRequest: {}", jsonString);
        return BodyPublishers.ofString(jsonString);
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
