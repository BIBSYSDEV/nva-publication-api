package no.unit.nva.publication.owner;

import static nva.commons.core.JsonUtils.objectMapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.List;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public class PublicationsByOwnerHandler extends ApiGatewayHandler<Void, PublicationsByOwnerResponse> {

    private final PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    public PublicationsByOwnerHandler() {
        this(new DynamoDBPublicationService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                objectMapper,
                new Environment()),
            new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param environment environment
     */
    public PublicationsByOwnerHandler(PublicationService publicationService,
                                      Environment environment) {
        super(Void.class, environment, LoggerFactory.getLogger(PublicationsByOwnerHandler.class));
        this.publicationService = publicationService;
    }

    @Override
    protected PublicationsByOwnerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        String owner = RequestUtil.getOwner(requestInfo);
        URI customerId = RequestUtil.getCustomerId(requestInfo);

        logger.info(String.format("Requested publications for owner with feideId=%s and publisher with customerId=%s",
            owner,
            customerId));

        List<PublicationSummary> publicationsByOwner = publicationService.getPublicationsByOwner(
            owner,
            customerId
        );

        return new PublicationsByOwnerResponse(publicationsByOwner);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationsByOwnerResponse output) {
        return HttpStatus.SC_OK;
    }
}
