package no.unit.nva;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatewayResponseTest {

    public static final String HEADER = "header";
    public static final String VALUE = "value";
    public static final String TEST_MESSAGE = "test message";
    public static final int STATUS_CODE = 200;

    @Test
    public void test() throws JsonProcessingException {
        GatewayResponse<String> gatewayResponse =
                new GatewayResponse<String>(TEST_MESSAGE, Map.of(HEADER, VALUE), STATUS_CODE);

        ObjectMapper objectMapper = PublicationHandler.createObjectMapper();
        GatewayResponse<String> processedGatewayResponse = objectMapper.readValue(
                objectMapper.writeValueAsString(gatewayResponse), GatewayResponse.class);

        assertEquals(gatewayResponse.getBody(), processedGatewayResponse.getBody());
        assertEquals(gatewayResponse.getStatusCode(), processedGatewayResponse.getStatusCode());
        assertEquals(gatewayResponse.getHeaders().get(HEADER), processedGatewayResponse.getHeaders().get(HEADER));

    }

}
