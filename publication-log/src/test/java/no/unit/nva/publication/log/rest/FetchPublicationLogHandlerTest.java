package no.unit.nva.publication.log.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FetchPublicationLogHandlerTest {

    private final Context context = new FakeContext();
    private ByteArrayOutputStream output;
    private FetchPublicationLogHandler handler;

    @BeforeEach
    public void setUp() {
        output = new ByteArrayOutputStream();
        handler = new FetchPublicationLogHandler();
    }

    @Test
    void shouldReturnEmptyPublicationLog() throws IOException {
        var publicationIdentifier = SortableIdentifier.next();

        handler.handleRequest(createRequest(publicationIdentifier), output, context);

        var response = GatewayResponse.fromOutputStream(output, PublicationLogResponse.class);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertTrue(response.getBodyObject(PublicationLogResponse.class).logEntries().isEmpty());
    }

    private InputStream createRequest(SortableIdentifier identifier) throws JsonProcessingException {
        var dtoObjectMapper = JsonUtils.dtoObjectMapper;
        return new HandlerRequestBuilder<InputStream>(dtoObjectMapper)
                   .withPathParameters(Map.of("publicationIdentifier", identifier.toString()))
                   .build();
    }
}