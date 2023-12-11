package no.unit.nva.publication.delete;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.RequestUtil.createInternalUserInstance;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.EditorPermissionStrategy;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.HttpStatus;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class DeletePublicationHandler extends ApiGatewayHandler<Void, Void> {

    public static final String LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS = "Lambda Function Invocation Result - Success";
    public static final String NVA_PUBLICATION_DELETE_SOURCE = "nva.publication.delete";

    public static final String API_HOST = "API_HOST";
    public static final String PUBLICATION = "publication";
    public static final String DUPLICATE_QUERY_PARAM = "duplicate";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final IdentityServiceClient identityServiceClient;
    private final EventBridgeClient eventBridgeClient;

    /**
     * Default constructor for DeletePublicationHandler.
     */
    @JacocoGenerated
    public DeletePublicationHandler() {
        this(ResourceService.defaultService(), TicketService.defaultService(), new Environment(), IdentityServiceClient.prepare(), defaultEventBridgeClient());
    }

    /**
     * Constructor for DeletePublicationHandler.
     *
     * @param resourceService resourceService
     * @param environment     environment
     * @param eventBridgeClient eventBridgeClient
     */
    public DeletePublicationHandler(ResourceService resourceService,TicketService ticketService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient,
                                    EventBridgeClient eventBridgeClient) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var userInstance = createUserInstanceFromRequest(requestInfo);
        var publicationIdentifier = RequestUtil.getIdentifier(requestInfo);

        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);

        switch (publication.getStatus()) {
            case DRAFT -> handleDraftDeletion(userInstance, publicationIdentifier);
            case PUBLISHED -> handleSoftDeletion(requestInfo, publication);
            case UNPUBLISHED -> handleHardDeletion(requestInfo, publication);
            default -> unsupportedPublicationForDeletion(publication);
        }

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }

    private static void validateHardDeletionRequest(RequestInfo requestInfo, Publication publication)
        throws UnauthorizedException {
        if (isEditor(requestInfo, publication) || canEditOwnInstitutionResources(requestInfo)) {
            return;
        }
        throw new UnauthorizedException();
    }

    private static boolean isEditor(RequestInfo requestInfo, Publication publication) {
        return EditorPermissionStrategy.fromRequestInfo(requestInfo).hasPermission(publication);
    }

    private static boolean canEditOwnInstitutionResources(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.name());
    }

    private static void unsupportedPublicationForDeletion(Publication publication) throws BadRequestException {
        throw new BadRequestException(
            String.format("Publication status %s is not supported for deletion", publication.getStatus()));
    }

    private static URI toPublicationUri(String duplicateIdentifier) {
        return UriWrapper.fromHost(new Environment().readEnv(API_HOST))
                   .addChild(PUBLICATION)
                   .addChild(duplicateIdentifier)
                   .getUri();
    }

    private void handleHardDeletion(RequestInfo requestInfo, Publication publication)
        throws UnauthorizedException, BadRequestException {
        validateHardDeletionRequest(requestInfo, publication);
        resourceService.deletePublication(publication);
    }

    private void handleDraftDeletion(UserInstance userInstance, SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        resourceService.markPublicationForDeletion(userInstance, publicationIdentifier);
    }

    private void handleSoftDeletion(RequestInfo requestInfo, Publication publication) throws ApiGatewayException {
        if (!PublicationPermissionStrategy.fromRequestInfo(requestInfo).hasPermissionToUnpublish(publication)) {
            throw new UnauthorizedException();
        }
        var duplicate = requestInfo.getQueryParameterOpt(DUPLICATE_QUERY_PARAM).orElse(null);
        resourceService.unpublishPublication(toPublicationWithDuplicate(duplicate, publication));
        persistNotification(publication);
        eventBridgeClient.putEvents(PutEventsRequest.builder().entries(PutEventsRequestEntry.builder()
           .source(NVA_PUBLICATION_DELETE_SOURCE)
           .detailType(LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS)
           .detail(DoiMetadataUpdateEvent.createUpdateDoiEvent(publication).toJsonString())
           .resources(publication.getIdentifier().toString())
           .build()).build());
    }

    private void persistNotification(Publication publication) throws ApiGatewayException {
        TicketEntry.requestNewTicket(publication, UnpublishRequest.class).persistNewTicket(ticketService);
    }

    private Publication toPublicationWithDuplicate(String duplicateIdentifier, Publication publication) {
        return nonNull(duplicateIdentifier) ? publication.copy()
                                                  .withDuplicateOf(toPublicationUri(duplicateIdentifier))
                                                  .build() : publication;
    }

    private UserInstance createUserInstanceFromRequest(RequestInfo requestInfo) throws ApiGatewayException {
        return requestInfo.clientIsThirdParty() ? createExternalUserInstance(requestInfo, identityServiceClient)
                   : createInternalUserInstance(requestInfo);
    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
                   .httpClientBuilder(UrlConnectionHttpClient.builder())
                   .build();    }
}
