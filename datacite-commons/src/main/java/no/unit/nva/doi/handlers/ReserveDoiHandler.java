package no.unit.nva.doi.handlers;

import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpHeaders.ACCEPT;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.DataCiteConfig;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.doi.model.DraftDoiDto;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class ReserveDoiHandler extends ApiGatewayHandler<Void, DoiResponse> {

    public static final String COLON = ":";
    public static final String NOT_DRAFT_STATUS_ERROR_MESSAGE = "Operation is not allowed, publication is not a draft";
    public static final String UNSUPPORTED_ROLE_ERROR_MESSAGE = "Only owner can reserve a doi";
    public static final String DATA_CITE_SECRET_NAME = "dataCiteCustomerSecrets";
    public static final String DATA_CITE_SECRET_KEY = "dataCiteCustomerSecrets";
    public static final String DATACITE_CONFIG_ERROR = "No datacite config for customer";
    public static final String DOI_PATH = "dois";
    public static final String APPLICATION_VND_API_JSON = "application/vnd.api+json";
    public static final String BAD_RESPONSE_ERROR_MESSAGE = "Bad response from DataCite";
    public static final String DATACITE_REST_HOST = "DATACITE_REST_HOST";
    public static final String BASIC_AUTH = "Basic ";
    protected static final String DOI_HOST = new Environment().readEnv("DOI_HOST");
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final ResourceService resourceService;
    private final SecretsReader secretsReader;
    private final HttpClient httpClient;
    private Publication publication;

    @JacocoGenerated
    public ReserveDoiHandler() {
        this(ResourceService.defaultService(), SecretsReader.defaultSecretsManagerClient(), getDefaultHttpClient(),
             new Environment());
    }

    public ReserveDoiHandler(ResourceService resourceService, SecretsManagerClient secretsManagerClient,
                             HttpClient httpClient, Environment environment) {
        super(Void.class, environment);
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.resourceService = resourceService;
        this.httpClient = httpClient;
    }

    public String responseToDoi(HttpResponse<String> response) {
        var uri = DraftDoiDto.fromJson(response.body()).getDoi();
        return new UriWrapper(UriWrapper.HTTPS, DOI_HOST)
                   .addChild(URI.create(uri).getPath())
                   .getUri()
                   .toString();
    }

    @Override
    protected DoiResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateReserveDoiRequest(requestInfo);
        var customerConfig = getConfig();
        return attempt(() -> generateDoi(customerConfig))
                   .map(DoiResponse::new)
                   .orElseThrow(failure -> new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, DoiResponse output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static boolean userIsOwnerOfPublication(RequestInfo requestInfo, Publication publication)
        throws ApiGatewayException {
        return RequestUtil.getOwner(requestInfo).equals(publication.getResourceOwner().getOwner());
    }

    private static boolean isNotADraft(Publication publication) {
        return !PublicationStatus.DRAFT.equals(publication.getStatus());
    }

    private static DataCiteConfig[] parseConfig(String config) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(config, DataCiteConfig[].class);
    }

    @JacocoGenerated
    private static HttpClient getDefaultHttpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }

    private static BodyPublisher createRequestBodyWithDoiPrefix(DataCiteConfig dataCiteConfig) {
        return BodyPublishers.ofString(DraftDoiDto.fromPrefix(dataCiteConfig.getCustomerDoiPrefix()).toJson());
    }

    private String generateDoi(DataCiteConfig customerConfig) {
        return attempt(() -> constructUri(customerConfig))
                   .map(uri -> createRequest(uri, customerConfig))
                   .map(this::send)
                   .map(this::responseToDoi)
                   .orElseThrow();
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest createRequest(URI uri, DataCiteConfig dataCiteConfig) {
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .POST(createRequestBodyWithDoiPrefix(dataCiteConfig))
                   .header(ACCEPT, APPLICATION_VND_API_JSON)
                   .headers(AUTHORIZATION_HEADER, constructAuthorizationString(dataCiteConfig))
                   .build();
    }

    private String constructAuthorizationString(DataCiteConfig dataCiteConfig) {
        return BASIC_AUTH + Base64.getEncoder()
                                .encodeToString((dataCiteConfig.getDataCiteMdsClientUsername() + COLON
                                                 + dataCiteConfig.getDataCiteMdsClientPassword()).getBytes());
    }

    private URI constructUri(DataCiteConfig customerConfig) {
        return UriWrapper.fromUri(environment.readEnv(DATACITE_REST_HOST))
                   .addChild(DOI_PATH)
                   .addChild(customerConfig.getCustomerDoiPrefix())
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    private DataCiteConfig getConfig() throws UnauthorizedException {
        var config = secretsReader.fetchSecret(DATA_CITE_SECRET_NAME, DATA_CITE_SECRET_KEY);
        return attempt(() -> parseConfig(config))
                   .map(Arrays::asList)
                   .map(this::extractCustomerConfig)
                   .orElseThrow(failure -> new UnauthorizedException(DATACITE_CONFIG_ERROR));
    }

    private DataCiteConfig extractCustomerConfig(List<DataCiteConfig> configList) {
        return configList.stream()
                   .filter(this::isCustomerConfig)
                   .collect(SingletonCollector.collect());
    }

    private boolean isCustomerConfig(DataCiteConfig config) {
        return publication.getPublisher().getId().equals(config.getCustomerId());
    }

    private void validateReserveDoiRequest(RequestInfo requestInfo) throws ApiGatewayException {
        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        publication = resourceService.getPublicationByIdentifier(identifier);
        if (isNotADraft(publication)) {
            throw new BadMethodException(NOT_DRAFT_STATUS_ERROR_MESSAGE);
        }
        if (!userIsOwnerOfPublication(requestInfo, publication)) {
            throw new UnauthorizedException(UNSUPPORTED_ROLE_ERROR_MESSAGE);
        }
    }
}
