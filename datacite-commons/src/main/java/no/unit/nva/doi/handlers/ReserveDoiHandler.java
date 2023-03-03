package no.unit.nva.doi.handlers;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.config.DataCiteConfig;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.secrets.SecretsReader;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class ReserveDoiHandler extends ApiGatewayHandler<Void, String> {

    public static final Void IS_VALID_REQUEST = null;
    public static final String NOT_DRAFT_STATUS_ERROR_MESSAGE = "Operation is not allowed, publication is not a draft";
    public static final String UNSUPPORTED_ROLE_ERROR_MESSAGE = "Only owner can reserve a doi";
    public static final String DATA_CITE_SECRET_NAME = "dataCiteCustomerSecrets";
    public static final String DATA_CITE_SECRET_KEY = "dataCiteCustomerSecrets";
    public static final String DATACITE_CONFIG_ERROR = "No datacite config for customer";
    private final ResourceService resourceService;
    private final SecretsReader secretsReader;
    private Publication publication;

    @JacocoGenerated
    public ReserveDoiHandler() {
        this(ResourceService.defaultService(), SecretsReader.defaultSecretsManagerClient(), new Environment());
    }

    public ReserveDoiHandler(ResourceService resourceService, SecretsManagerClient secretsManagerClient,
                             Environment environment) {
        super(Void.class, environment);
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.resourceService = resourceService;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        validateReserveDoiRequest(requestInfo);
        var customerConfig = getConfig();
        return IS_VALID_REQUEST.toString();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return null;
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

    private boolean exceptionInstanceOfRuntimeException(Exception exception) {
        return exception instanceof RuntimeException;
    }
}
