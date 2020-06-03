package no.unit.nva.publication.query;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.ListPublicationsResponse;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static nva.commons.utils.JsonUtils.objectMapper;

public class ListPublishedPublicationsHandler extends ApiGatewayHandler<Void, ListPublicationsResponse> {

    private final PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    public ListPublishedPublicationsHandler() {
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
    public ListPublishedPublicationsHandler(PublicationService publicationService,
                                            Environment environment) {
        super(Void.class, environment, LoggerFactory.getLogger(ListPublishedPublicationsHandler.class));
        this.publicationService = publicationService;
    }

    @Override
    protected ListPublicationsResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        Map<String, AttributeValue> lastEvaluatedKey = RequestUtil.getLastKey(requestInfo);
        int pageSize = RequestUtil.getPageSize(requestInfo);

        logger.info(String.format("Requested latest modified publications starting from lastEvaluatedKey=%s and pagesize=%s",
            lastEvaluatedKey,
            pageSize));

        ListPublicationsResponse publicationsResponse = publicationService.listPublishedPublicationsByDate(lastEvaluatedKey, pageSize);

        return publicationsResponse;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, ListPublicationsResponse output) {
        return HttpStatus.SC_OK;
    }
}
