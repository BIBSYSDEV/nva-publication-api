package no.unit.nva.publication.fetch;

import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.RequestUtil.createInternalUserInstance;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationsByOwnerHandler extends ApiGatewayHandler<Void, PublicationsByOwnerResponse> {
    
    private static final Logger logger = LoggerFactory.getLogger(PublicationsByOwnerHandler.class);
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

    @JacocoGenerated
    public PublicationsByOwnerHandler() {
        this(new ResourceService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                Clock.systemDefaultZone()),
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
    protected PublicationsByOwnerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var userInstance = createUserInstanceFromLoginInformation(requestInfo);

        logger.info(String.format("Requested publications for owner with username/feideId=%s and publisher with "
                                  + "customerId=%s",
            userInstance.getUsername(),
            userInstance.getOrganizationUri())
        );
        
        List<PublicationSummary> publicationsByOwner;
        publicationsByOwner = resourceService.getPublicationsByOwner(userInstance)
                                  .stream()
                                  .map(PublicationSummary::create)
                                  .collect(Collectors.toList());
        
        return new PublicationsByOwnerResponse(publicationsByOwner);
    }

    private UserInstance createUserInstanceFromLoginInformation(RequestInfo requestInfo) throws ApiGatewayException {
        return requestInfo.clientIsThirdParty() ?
                   createExternalUserInstance(requestInfo, identityServiceClient)
                   : createInternalUserInstance(requestInfo);
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationsByOwnerResponse output) {
        return HttpStatus.SC_OK;
    }
}
