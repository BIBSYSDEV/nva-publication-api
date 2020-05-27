package no.unit.nva.publication.owner;

import static nva.commons.utils.JsonUtils.objectMapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.unit.nva.model.util.OrgNumberMapper;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
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
        String orgNumber = RequestUtil.getOrgNumber(requestInfo);

        logger.info(String.format("Requested publications for owner with feideId=%s and publisher with orgNumber=%s",
            owner,
            orgNumber));

        List<PublicationSummary> publicationsByOwner = publicationService.getPublicationsByOwner(
            owner,
            OrgNumberMapper.toCristinId(orgNumber)
        );

        return new PublicationsByOwnerResponse(publicationsByOwner);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationsByOwnerResponse output) {
        return HttpStatus.SC_OK;
    }
}
