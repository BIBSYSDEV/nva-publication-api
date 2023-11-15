package no.unit.nva.publication.delete;

import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.RequestUtil.createInternalUserInstance;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.publication.PublicationPermissionStrategy;
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

public class DeletePublicationHandler extends ApiGatewayHandler<Void, Void> {
    
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;
    private PublicationPermissionStrategy publicationPermissionStrategy;

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
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
        this.publicationPermissionStrategy = new PublicationPermissionStrategy();
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var userInstance = createUserInstanceFromRequest(requestInfo);
        var publicationIdentifier = RequestUtil.getIdentifier(requestInfo);

        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        var publicationStatus = publication.getStatus();

        switch (publicationStatus) {
            case PUBLISHED:
                if (!publicationPermissionStrategy.hasPermissionToUnpublish(requestInfo, publication)) {
                    throw new UnauthorizedException();
                }
                resourceService.unpublishPublication(publication);
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

    private UserInstance createUserInstanceFromRequest(RequestInfo requestInfo) throws ApiGatewayException {
        return requestInfo.clientIsThirdParty()
                   ? createExternalUserInstance(requestInfo, identityServiceClient)
                   : createInternalUserInstance(requestInfo);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }
    
    @JacocoGenerated
    private static ResourceService defaultService() {
        return new ResourceService(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }
}
