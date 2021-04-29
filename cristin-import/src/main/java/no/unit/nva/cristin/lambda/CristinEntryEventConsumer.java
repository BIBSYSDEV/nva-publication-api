package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;

public class CristinEntryEventConsumer extends EventHandler<CristinObject, Void> {

    private final AmazonDynamoDB dynamoDbClient;
    private final ResourceService resourceService;

    protected CristinEntryEventConsumer(AmazonDynamoDB dynamoDbClient) {
        super(CristinObject.class);
        this.dynamoDbClient = dynamoDbClient;
        resourceService = new ResourceService(dynamoDbClient, Clock.systemDefaultZone());
    }

    @Override
    protected Void processInput(CristinObject input, AwsEventBridgeEvent<CristinObject> event, Context context) {
        Publication publication = input.toPublication();
        attempt(() -> resourceService.createPublication(publication))
            .orElseThrow(fail -> new RuntimeException(fail.getException()));
        return null;
    }
}
