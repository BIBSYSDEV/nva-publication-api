package no.unit.nva.publication.events.handlers.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.create.CreatePublicationRequest;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatePublishedPublicationHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    CreatePublishedPublicationHandler handler = new CreatePublishedPublicationHandler();
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnAPublicationResponseWhenRequestIsReceived()
        throws IOException {
        var publication = new Publication();
        var request = CreatePublicationRequest.fromPublication(publication);
        var inputStream = createRequest(request);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        var responseString = outputStream.toString();
        var response = JsonUtils.dtoObjectMapper.readValue(responseString, PublicationResponse.class);

        assertThat(response, is(not(nullValue())));
    }

    private InputStream createRequest(CreatePublicationRequest request) {
        return EventBridgeEventBuilder.sampleEvent(request);
    }
}
