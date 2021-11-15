package no.unit.nva.publication.events.handlers.dynamodbstream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DynamodbStreamToEventBridgeHandlerTest {

    public static final String DYNAMODB_STREAM_EVENT =
        IoUtils.stringFromResources(Path.of("dynamodbstreams/event.json"));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void handleRequestWritesToConsoleOnValidEvent() throws Exception {
        EventPublisher eventPublisher = event -> {
            try {
                new ObjectMapper().writeValue(System.out, event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        DynamodbStreamToEventBridgeHandler handler = createHandler(eventPublisher);
        Context context = Mockito.mock(Context.class);
        DynamodbEvent event = objectMapper.readValue(DYNAMODB_STREAM_EVENT, DynamodbEvent.class);
        handler.handleRequest(event, context);
    }

    private DynamodbStreamToEventBridgeHandler createHandler(EventPublisher eventPublisher) {
        return new DynamodbStreamToEventBridgeHandler(eventPublisher);
    }
}
