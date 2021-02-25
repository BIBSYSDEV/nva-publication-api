package no.unit.nva.doi.requests.handlers;

import static no.unit.useraccessserivce.accessrights.AccessRight.APPROVE_DOI_REQUEST;
import static no.unit.useraccessserivce.accessrights.AccessRight.REJECT_DOI_REQUEST;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Clock;
import java.util.Collections;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDoiRequestStatusHandler extends ApiGatewayHandler<ApiUpdateDoiRequest, Void> {

    public static final String API_PUBLICATION_PATH_IDENTIFIER = "publicationIdentifier";
    public static final String INVALID_PUBLICATION_ID_ERROR = "Invalid publication id: ";
    public static final String API_SCHEME_ENV_VARIABLE = "API_SCHEME";
    public static final String API_HOST_ENV_VARIABLE = "API_HOST";
    private static final String LOCATION_TEMPLATE_PUBLICATION = "%s://%s/publication/%s";

    private static final Logger logger = LoggerFactory.getLogger(UpdateDoiRequestStatusHandler.class);
    private final String apiScheme;
    private final String apiHost;
    private final DoiRequestService doiRequestService;

    @JacocoGenerated
    public UpdateDoiRequestStatusHandler() {
        this(defaultEnvironment(), defaultService());
    }

    public UpdateDoiRequestStatusHandler(Environment environment,
                                         DoiRequestService doiRequestService) {
        super(ApiUpdateDoiRequest.class, environment, logger);
        this.apiHost = environment.readEnv(API_HOST_ENV_VARIABLE);
        this.apiScheme = environment.readEnv(API_SCHEME_ENV_VARIABLE);
        this.doiRequestService = doiRequestService;
    }

    @Override
    protected Void processInput(ApiUpdateDoiRequest input,
                                RequestInfo requestInfo,
                                Context context)
        throws ApiGatewayException {

        String requestInfoJson = attempt(() -> JsonUtils.objectMapper.writeValueAsString(requestInfo)).orElseThrow();
        logger.info("RequestInfo:\n" + requestInfoJson);

        try {
            input.validate();
            SortableIdentifier publicationIdentifier = getPublicationIdentifier(requestInfo);
            UserInstance userInstance = createUserInstance(requestInfo);
            validateUser(requestInfo);
            updateDoiRequestStatus(userInstance, input.getDoiRequestStatus(), publicationIdentifier);
            updateContentLocationHeader(publicationIdentifier);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(ApiUpdateDoiRequest input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }

    @JacocoGenerated
    private static DoiRequestService defaultService() {
        return new DoiRequestService(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }

    @JacocoGenerated
    private static Environment defaultEnvironment() {
        return new Environment();
    }

    private void validateUser(RequestInfo requestInfo) throws NotAuthorizedException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }

    private boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return
            !(
                requestInfo.userHasAccessRight(APPROVE_DOI_REQUEST.toString())
                && requestInfo.userHasAccessRight(REJECT_DOI_REQUEST.toString())
            );
    }

    private UserInstance createUserInstance(RequestInfo requestInfo) {
        String user = requestInfo.getFeideId().orElse(null);
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        return new UserInstance(user, customerId);
    }

    private void updateDoiRequestStatus(UserInstance userInstance,
                                        DoiRequestStatus newDoiRequestStatus,
                                        SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        doiRequestService.updateDoiRequest(userInstance, publicationIdentifier, newDoiRequestStatus);
    }

    private void updateContentLocationHeader(SortableIdentifier publicationIdentifier) {
        setAdditionalHeadersSupplier(() ->
                                         Collections.singletonMap(HttpHeaders.LOCATION,
                                             getContentLocation(publicationIdentifier)));
    }

    private String getContentLocation(SortableIdentifier publicationID) {
        return String.format(LOCATION_TEMPLATE_PUBLICATION, apiScheme, apiHost, publicationID.toString());
    }

    private SortableIdentifier getPublicationIdentifier(RequestInfo requestInfo) throws BadRequestException {
        String publicationIdentifierString = requestInfo.getPathParameter(API_PUBLICATION_PATH_IDENTIFIER);
        return attempt(() -> new SortableIdentifier(publicationIdentifierString))
                   .orElseThrow(
                       fail -> new BadRequestException(INVALID_PUBLICATION_ID_ERROR + publicationIdentifierString));
    }
}
