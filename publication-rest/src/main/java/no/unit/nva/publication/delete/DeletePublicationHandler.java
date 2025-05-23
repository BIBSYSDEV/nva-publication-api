package no.unit.nva.publication.delete;

import static no.unit.nva.model.PublicationOperation.DELETE;
import static no.unit.nva.publication.RequestUtil.createUserInstanceFromRequest;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class DeletePublicationHandler extends ApiGatewayHandler<Void, Void> {

    public static final String LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS =
        "Lambda Function Invocation Result - Success";
    public static final String NVA_PUBLICATION_DELETE_SOURCE = "nva.publication.delete";
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

    /**
     * Default constructor for DeletePublicationHandler.
     */
    @JacocoGenerated
    public DeletePublicationHandler() {
        this(ResourceService.defaultService(), new Environment(), IdentityServiceClient.prepare());
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
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var userInstance = createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);

        var resource = resourceService.getResourceByIdentifier(resourceIdentifier);

        if (resource.getStatus() == PublicationStatus.DRAFT) {
            PublicationPermissions.create(resource, userInstance).authorize(DELETE);
            resourceService.deleteDraftPublication(userInstance, resourceIdentifier);
        } else {
            unsupportedPublicationForDeletion(resource);
        }

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }

    private static void unsupportedPublicationForDeletion(Resource resource) throws BadRequestException {
        throw new BadRequestException(
            String.format("Publication status %s is not supported for deletion", resource.getStatus()));
    }
}
