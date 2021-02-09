package no.unit.nva.doi.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.doi.UpdateDoiStatusProcess;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.doi.update.dto.DoiUpdateHolder;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

public class UpdateDoiStatusHandler extends DestinationsEventBridgeEventHandler<DoiUpdateHolder, Void> {

    public static final Void SUCCESSESFULLY_HANDLED_EVENT = null;
    private final ResourceService resourceService;

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
     * @param resourceService publicationService
     */
    public UpdateDoiStatusHandler(ResourceService resourceService) {
        super(DoiUpdateHolder.class);
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInputPayload(DoiUpdateHolder input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<DoiUpdateHolder>> event,
                                       Context context) {
        new UpdateDoiStatusProcess(resourceService, input).updateDoiStatus();
        return SUCCESSESFULLY_HANDLED_EVENT;
    }

    @JacocoGenerated
    private static ResourceService defaultDynamoDBPublicationService() {
        return new ResourceService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            Clock.systemDefaultZone());
    }
}
