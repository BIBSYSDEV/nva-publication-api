package no.unit.nva.publication.events.handlers.initialization;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InitializationHandlerTest {
    
    public static final Context CONTEXT = mock(Context.class);
    public static final String SAMPLE_PIPELINE_EVENT_FROM_AWS_DOCUMENTATION =
        "initialization/pipeline_succeeded_event.json";
    public static final String PIPELINE_NAME_IN_RESOURCES_FILE = "myPipeline";
    private InitializationHandler handler;
    private ByteArrayOutputStream outputStream;
    
    @BeforeEach
    public void init() {
        this.handler = new InitializationHandler();
        this.outputStream = new ByteArrayOutputStream();
    }
    
    @Test
    void shouldLogReceivedEventWhenHandlerIsActivated() {
        TestAppender logger = LogUtils.getTestingAppenderForRootLogger();
        var sampleEvent = stringFromResources(Path.of(SAMPLE_PIPELINE_EVENT_FROM_AWS_DOCUMENTATION));
        handler.handleRequest(stringToStream(sampleEvent), outputStream, CONTEXT);
        assertThat(logger.getMessages(), containsString(PIPELINE_NAME_IN_RESOURCES_FILE));
    }
}
