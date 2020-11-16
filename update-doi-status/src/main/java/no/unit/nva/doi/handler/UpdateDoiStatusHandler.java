package no.unit.nva.doi.handler;

import static nva.commons.utils.JsonUtils.objectMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.doi.UpdateDoiStatusProcess;
import no.unit.nva.doi.model.DoiUpdateHolder;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;

public class UpdateDoiStatusHandler extends DestinationsEventBridgeEventHandler<DoiUpdateHolder, Void> {

    private final PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public UpdateDoiStatusHandler() {
        this(defaultDynamoDBPublicationService());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param publicationService publicationService
     */
    public UpdateDoiStatusHandler(PublicationService publicationService) {
        super(DoiUpdateHolder.class);
        this.publicationService = publicationService;
    }

    @Override
    protected Void processInputPayload(DoiUpdateHolder input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<DoiUpdateHolder>> event,
                                       Context context) {
        new UpdateDoiStatusProcess(publicationService, input).updateDoiStatus();
        return null;
    }

    @JacocoGenerated
    private static DynamoDBPublicationService defaultDynamoDBPublicationService() {
        return new DynamoDBPublicationService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            objectMapper,
            new Environment());
    }
}
