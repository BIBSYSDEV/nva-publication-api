package no.unit.nva.publication.delete;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.RequestUtil.createInternalUserInstance;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.HttpStatus;

public class DeletePublicationHandler extends ApiGatewayHandler<Void, Void> {

    public static final String API_HOST = "API_HOST";
    public static final String PUBLICATION = "publication";
    public static final String DUPLICATE_QUERY_PARAM = "duplicate";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final IdentityServiceClient identityServiceClient;

    /**
     * Default constructor for DeletePublicationHandler.
     */
    @JacocoGenerated
    public DeletePublicationHandler() {
        this(ResourceService.defaultService(), TicketService.defaultService(), new Environment(),
             IdentityServiceClient.prepare());
    }

    /**
     * Constructor for DeletePublicationHandler.
     *
     * @param resourceService resourceService
     * @param environment     environment
     */
    public DeletePublicationHandler(ResourceService resourceService, TicketService ticketService,
                                    Environment environment, IdentityServiceClient identityServiceClient) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var userInstance = createUserInstanceFromRequest(requestInfo);
        var publicationIdentifier = RequestUtil.getIdentifier(requestInfo);

        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        var publicationStatus = publication.getStatus();

        switch (publicationStatus) {
            case PUBLISHED:
                if (!PublicationPermissionStrategy.fromRequestInfo(requestInfo).hasPermissionToUnpublish(publication)) {
                    throw new UnauthorizedException();
                }
                var duplicate = requestInfo.getQueryParameterOpt(DUPLICATE_QUERY_PARAM).orElse(null);
                resourceService.unpublishPublication(toPublicationWithDuplicate(duplicate, publication));
                persistNotification(publication);
                break;
            case DRAFT:
                resourceService.markPublicationForDeletion(userInstance, publicationIdentifier);
                break;
            default:
                throw new BadRequestException(
                    String.format("Publication status %s is not supported for deletion", publicationStatus));
        }

        return null;
    }

    private void persistNotification(Publication publication) throws ApiGatewayException {
        TicketEntry.requestNewTicket(publication, UnpublishRequest.class).persistNewTicket(ticketService);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }

    private static URI toPublicationUri(String duplicateIdentifier) {
        return UriWrapper.fromHost(new Environment().readEnv(API_HOST))
                   .addChild(PUBLICATION)
                   .addChild(duplicateIdentifier)
                   .getUri();
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
}
