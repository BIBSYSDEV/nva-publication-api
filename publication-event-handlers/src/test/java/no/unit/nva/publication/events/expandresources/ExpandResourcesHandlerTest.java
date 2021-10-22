package no.unit.nva.publication.events.expandresources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExpandResourcesHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final String RESOURCE_IDENTIFIER_IN_SAMPLE_FILE = "017c93559df0-541e2774-d27f-49c9-a234-a26cde19d204";
    String sampleEvent = IoUtils.stringFromResources(Path.of("expandResources/resource-update-sample.json"));
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        this.output = new ByteArrayOutputStream();
    }

    @Test
    void shouldLogEventThatIsResourceUpdate() {
        var testingAppender = LogUtils.getTestingAppender(ExpandResourcesHandler.class);
        var request = IoUtils.stringToStream(sampleEvent);
        var expandResourceHandler = new ExpandResourcesHandler();
        assertDoesNotThrow(() -> expandResourceHandler.handleRequest(request, output, CONTEXT));
        assertThat(testingAppender.getMessages(), containsString(RESOURCE_IDENTIFIER_IN_SAMPLE_FILE));
    }
}
