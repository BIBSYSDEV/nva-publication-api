package no.unit.nva.publication.fetch;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
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

    @JacocoGenerated
    public PublicationsByOwnerHandler() {
        this(new ResourceService(
                 AmazonDynamoDBClientBuilder.defaultClient(),
                 Clock.systemDefaultZone()),
             new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param environment environment
     */
    public PublicationsByOwnerHandler(ResourceService resourceService,
                                      Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
    }

    @Override
    protected PublicationsByOwnerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        String owner = RequestUtil.getOwner(requestInfo);
        URI customerId = requestInfo.getCurrentCustomer();
        UserInstance userInstance = UserInstance.create(owner, customerId);
        logger.info(String.format("Requested publications for owner with feideId=%s and publisher with customerId=%s",
                                  owner,
                                  customerId));

        List<PublicationSummary> publicationsByOwner;
        publicationsByOwner = resourceService.getPublicationsByOwner(userInstance)
                                  .stream()
                                  .map(PublicationSummary::create)
                                  .collect(Collectors.toList());

        return new PublicationsByOwnerResponse(publicationsByOwner);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationsByOwnerResponse output) {
        return HttpStatus.SC_OK;
    }
}
