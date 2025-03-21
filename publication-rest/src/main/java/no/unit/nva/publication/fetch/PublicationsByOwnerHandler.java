package no.unit.nva.publication.fetch;

import static no.unit.nva.publication.RequestUtil.createUserInstanceFromRequest;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class PublicationsByOwnerHandler extends ApiGatewayHandler<Void, PublicationsByOwnerResponse> {
    
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

    @JacocoGenerated
    public PublicationsByOwnerHandler() {
        this(ResourceService.defaultService(),
             new Environment(),
             IdentityServiceClient.prepare());
    }
    
    /**
     * Constructor for MainHandler.
     *
     * @param environment environment
     */
    public PublicationsByOwnerHandler(ResourceService resourceService,
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
    protected PublicationsByOwnerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var userInstance = createUserInstanceFromRequest(requestInfo, identityServiceClient);

        return new PublicationsByOwnerResponse(resourceService.getPublicationSummaryByOwner(userInstance));
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationsByOwnerResponse output) {
        return HttpStatus.SC_OK;
    }
}
