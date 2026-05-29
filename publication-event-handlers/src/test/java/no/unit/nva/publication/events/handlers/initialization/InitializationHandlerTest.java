package no.unit.nva.publication.events.handlers.initialization;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InitializationHandlerTest {

  public static final Context CONTEXT = null;
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
    var logRecorder = LogRecorder.forRoot(InitializationHandlerTest.class);
    var sampleEvent = stringFromResources(Path.of(SAMPLE_PIPELINE_EVENT_FROM_AWS_DOCUMENTATION));
    handler.handleRequest(stringToStream(sampleEvent), outputStream, CONTEXT);
    assertThat(logRecorder.messages(), hasItem(containsString(PIPELINE_NAME_IN_RESOURCES_FILE)));
  }
}
