package no.unit.nva.doi.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import no.unit.nva.doi.publisher.EventPublisher;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamodbEventFanoutStreamRecordsEventBridgeHandlerTest {

    public static final String DYNAMODB_STREAM_EVENT = "src/test/resources/event.json";
    private static final String WILDCARD = "*";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Environment environmentMock;

    @BeforeEach
    void setUp() {
        environmentMock = mock(Environment.class);
        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);

    }

    @Test
    public void handleRequestWritesToConsoleOnValidEvent() throws Exception {
        EventPublisher eventPublisher = event -> {
            try {
                new ObjectMapper().writeValue(System.out, event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        DynamodbEventFanoutStreamRecordsEventBridgeHandler handler = new DynamodbEventFanoutStreamRecordsEventBridgeHandler(eventPublisher);
        Context context = Mockito.mock(Context.class);
        File eventFile = new File(DYNAMODB_STREAM_EVENT);
        DynamodbEvent event = objectMapper.readValue(eventFile, DynamodbEvent.class);

        handler.handleRequest(event, context);
    }
}
