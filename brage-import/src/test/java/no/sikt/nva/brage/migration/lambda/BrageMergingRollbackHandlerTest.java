package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.lambda.BrageMergingRollbackHandler.TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BrageMergingRollbackHandlerTest {
    
    private BrageMergingRollbackHandler brageMergingRollbackHandler;
    
    private final Context CONTEXT = mock(Context.class);
    
    @BeforeEach
    void init(){
        brageMergingRollbackHandler = new BrageMergingRollbackHandler();
    }
    
    @Test
    void dummy(){
        var dummyEventReference = new EventReference(TOPIC, null, randomUri());
        var awsEventBridgeEvent = createAwsEventBridgeEvent(dummyEventReference);
        brageMergingRollbackHandler.processInput(dummyEventReference, awsEventBridgeEvent, CONTEXT);
    }

    private AwsEventBridgeEvent<EventReference> createAwsEventBridgeEvent(EventReference eventReference) {
        var awsEventBridgeEvent = new AwsEventBridgeEvent<EventReference>();
        awsEventBridgeEvent.setDetail(eventReference);
        return awsEventBridgeEvent;
    }
}
