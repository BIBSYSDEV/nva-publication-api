package no.unit.nva.publication.delete;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.RequestUtil.createInternalUserInstance;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Clock;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class DeletePublicationHandler extends ApiGatewayHandler<DeletePublicationRequest, Void> {
    
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

    /**
     * Default constructor for DeletePublicationHandler.
     */
    @JacocoGenerated
    public DeletePublicationHandler() {
        this(defaultService(), new Environment(), IdentityServiceClient.prepare());
    }
    
    /**
     * Constructor for DeletePublicationHandler.
     *
     * @param resourceService resourceService
     * @param environment     environment
     */
    public DeletePublicationHandler(ResourceService resourceService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient) {
        super(DeletePublicationRequest.class, environment);
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected Void processInput(DeletePublicationRequest input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var userInstance = createUserInstanceFromRequest(requestInfo);
        var publicationIdentifier = RequestUtil.getIdentifier(requestInfo);

        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        var publicationStatus = publication.getStatus();

        switch (publicationStatus) {
            case PUBLISHED:
                if (!PublicationPermissionStrategy.fromRequestInfo(requestInfo).hasPermissionToUnpublish(publication)) {
                    throw new UnauthorizedException();
                }
                resourceService.unpublishPublication(toPublicationWithDuplicate(input, publication));
                break;
            case DRAFT:
                resourceService.markPublicationForDeletion(userInstance, publicationIdentifier);
                break;
            default:
                throw new BadRequestException(String.format("Publication status %s is not supported for deletion",
                                                            publicationStatus));
        }

        return null;
    }

    private Publication toPublicationWithDuplicate(DeletePublicationRequest input, Publication publication) {
        return nonNull(input) ? publication.copy().withDuplicateOf(input.duplicateOf()).build() : publication;
    }

    private UserInstance createUserInstanceFromRequest(RequestInfo requestInfo) throws ApiGatewayException {
        return requestInfo.clientIsThirdParty()
                   ? createExternalUserInstance(requestInfo, identityServiceClient)
                   : createInternalUserInstance(requestInfo);
    }

    @Override
    protected Integer getSuccessStatusCode(DeletePublicationRequest input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }
    
    @JacocoGenerated
    private static ResourceService defaultService() {
        return new ResourceService(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }
}
