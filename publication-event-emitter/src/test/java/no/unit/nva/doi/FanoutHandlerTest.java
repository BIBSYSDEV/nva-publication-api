package no.unit.nva.doi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import no.unit.nva.doi.lambda.FanoutHandler;
import no.unit.nva.doi.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FanoutHandlerTest {

    public static final String DYNAMODB_STREAM_EVENT = "src/test/resources/event.json";
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
        FanoutHandler handler = new FanoutHandler(eventPublisher);
        Context context = Mockito.mock(Context.class);
        File eventFile = new File(DYNAMODB_STREAM_EVENT);
        DynamodbEvent event = objectMapper.readValue(eventFile, DynamodbEvent.class);

        handler.handleRequest(event, context);
    }
}
