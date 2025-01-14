package no.unit.nva.publication.log.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class FetchPublicationLogHandler extends ApiGatewayHandler<Void, PublicationLogResponse> {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private final ResourceService resourceService;

    @JacocoGenerated
    public FetchPublicationLogHandler() {
        this(ResourceService.defaultService());
    }

    public FetchPublicationLogHandler(ResourceService resourceService) {
        super(Void.class);
        this.resourceService = resourceService;
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context) {
        //NO OP
    }

    @Override
    protected PublicationLogResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var resourceIdentifier = new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER));
        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService);

        validateUserRights(requestInfo, resource);

        var logEntries = resource.fetchLogEntries(resourceService);
        return PublicationLogResponse.fromLogEntries(logEntries);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationLogResponse publicationLogResponse) {
        return HTTP_OK;
    }

    private static void validateUserRights(RequestInfo requestInfo, Resource resource)
        throws UnauthorizedException, ForbiddenException {
        if (userHasNoAccessToLog(requestInfo, resource)) {
            throw new ForbiddenException();
        }
    }

    private static boolean userHasNoAccessToLog(RequestInfo requestInfo, Resource resource)
        throws UnauthorizedException {
        return !PublicationPermissionStrategy.create(resource.toPublication(),
                                                     UserInstance.fromRequestInfo(requestInfo))
                    .allowsAction(PublicationOperation.UPDATE);
    }
}
