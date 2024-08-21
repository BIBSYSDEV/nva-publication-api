package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

public class UpdatePublicationsInBatchesHandler extends EventHandler<MyEvent, Void> {

    private final EventBridgeClient eventBridgeClient;
    private final SearchService searchService;
    private final ResourceService ResourceService;

    protected UpdatePublicationsInBatchesHandler(EventBridgeClient eventBridgeClient, SearchService searchService,
                                                 ResourceService resourceService) {
        super(MyEvent.class);
        this.eventBridgeClient = eventBridgeClient;
        this.searchService = searchService;
        ResourceService = resourceService;
    }

    @Override
    protected Void processInput(MyEvent input, AwsEventBridgeEvent<MyEvent> awsEventBridgeEvent, Context context) {
        return null;
    }
}
