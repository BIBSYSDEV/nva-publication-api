package no.unit.nva.publication.delete;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static nva.commons.utils.JsonUtils.objectMapper;

public class DeletePublicationHandler extends ApiGatewayHandler<Void,Void> {

    private final PublicationService publicationService;

    private static final Logger logger = LoggerFactory.getLogger(DeletePublicationHandler.class);

    /**
     * Default constructor for DeletePublicationHandler.
     */
    @JacocoGenerated
    public DeletePublicationHandler() {
        this(new DynamoDBPublicationService(
                        AmazonDynamoDBClientBuilder.defaultClient(),
                        objectMapper,
                        new Environment()),
                new Environment());
    }

    /**
     * Constructor for DeletePublicationHandler.
     *
     * @param publicationService    publicationService
     * @param environment   environment
     */
    public DeletePublicationHandler(PublicationService publicationService, Environment environment) {
        super(Void.class, environment, logger);
        this.publicationService = publicationService;
    }
    
    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        UUID identifier = RequestUtil.getIdentifier(requestInfo);
        publicationService.markPublicationForDeletion(identifier);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }
}
