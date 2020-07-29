package no.unit.nva.publication.query;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
import nva.commons.utils.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nva.commons.utils.JsonUtils.objectMapper;

public class ListPublishedPublicationsHandler extends ApiGatewayHandler<Void, PublishedPublicationsResponse> {

    public static final String AWS_REGION = "AWS_REGION";
    private final PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public ListPublishedPublicationsHandler() {
        this(getPublicationService(),
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
    protected PublishedPublicationsResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        int pageSize = RequestUtil.getPageSize(requestInfo);

        logger.debug(String.format("Requested latest modified publications pagesize=%d", pageSize));

        List<PublicationSummary> publicationsResponse = publicationService.listPublishedPublicationsByDate(pageSize);

        return new PublishedPublicationsResponse(publicationsResponse);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublishedPublicationsResponse output) {
        return HttpStatus.SC_OK;
    }

    private static DynamoDBPublicationService getPublicationService() {
        Environment environment = new Environment();
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(environment.readEnv(AWS_REGION))
                .build();
        return new DynamoDBPublicationService(amazonDynamoDB, objectMapper, environment);
    }
}
