package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.OutputStream;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.Test;

class ScopusDeletionEventHandlerTest {
    
    public static final Context CONTEXT = mock(Context.class);
    private final OutputStream outputStream = OutputStream.nullOutputStream();
    
    @Test
    void shouldDoNothingElseButLogReceivedMessageUntilFunctionalityIsSpecified() {
        
        var eventBody = new ScopusDeleteEventBody(randomString());
        var event = EventBridgeEventBuilder.sampleEvent(eventBody);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var handler = new ScopusDeletionEventHandler();
        handler.handleRequest(event, outputStream, CONTEXT);
        assertThat(logger.getMessages(), containsString(eventBody.getScopusIdentifier()));
    }
}