package no.unit.nva.publication.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;

public class PublishPublicationHandler extends ApiGatewayHandler<Void, Void> {

    private final ResourceService resourceService;

    @JacocoGenerated
    public PublishPublicationHandler() {
        this(ResourceService.defaultService());
    }

    public PublishPublicationHandler(ResourceService resourceService) {
        super(Void.class);
        this.resourceService = resourceService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //No input to validate
    }

    @Override
    protected Void processInput(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);

        publishResource(resourceIdentifier, userInstance);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HTTP_ACCEPTED;
    }

    private static void validatePermissions(Resource resource, UserInstance userInstance) throws ForbiddenException {
        var permissionStrategy = PublicationPermissions.create(resource.toPublication(), userInstance);
        if (!permissionStrategy.allowsAction(PublicationOperation.UPDATE)) {
            throw new ForbiddenException();
        }
    }

    private void publishResource(SortableIdentifier resourceIdentifier, UserInstance userInstance)
        throws ApiGatewayException {
        try {
            var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService);
            validatePermissions(resource, userInstance);
            resource.publish(resourceService, userInstance);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception exception) throws ApiGatewayException {
        if (exception instanceof IllegalStateException) {
            throw new BadRequestException(exception.getMessage());
        } else if (exception instanceof ApiGatewayException apiGatewayException) {
            throw apiGatewayException;
        } else {
            throw new BadGatewayException(exception.getMessage());
        }
    }
}
