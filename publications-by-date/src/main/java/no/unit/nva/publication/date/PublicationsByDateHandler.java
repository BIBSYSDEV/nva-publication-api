package no.unit.nva.publication.date;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
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

import java.util.List;
import java.util.Optional;

import static nva.commons.utils.JsonUtils.objectMapper;

public class PublicationsByDateHandler extends ApiGatewayHandler<Void, PublicationsByDateResponse> {

    private final PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    public PublicationsByDateHandler() {
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
    public PublicationsByDateHandler(PublicationService publicationService,
                                      Environment environment) {
        super(Void.class, environment, LoggerFactory.getLogger(PublicationsByDateHandler.class));
        this.publicationService = publicationService;
    }

    @Override
    protected PublicationsByDateResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        Optional<String> lastEvaluatedKey = RequestUtil.getLastKey(requestInfo);
        int pageSize = RequestUtil.getPageSize(requestInfo);

        logger.info(String.format("Requested latest modified publications starting from lastEvaluatedKey=%s and pagesize=%s",
            lastEvaluatedKey,
            pageSize));

        List<PublicationSummary> publications = publicationService.getPublicationsByModifiedDate(lastEvaluatedKey, pageSize);

        return new PublicationsByDateResponse(publications);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationsByDateResponse output) {
        return HttpStatus.SC_OK;
    }
}
