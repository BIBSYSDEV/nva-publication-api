package no.unit.nva.publication.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import no.unit.nva.commons.json.JsonUtils;
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
        Publication publication = new Publication();
        var request = CreatePublicationRequest.fromPublication(publication);
        var inputStream = createRequest(request);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        GatewayResponse<PublicationResponse> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getBodyObject(PublicationResponse.class), is(not(nullValue())));
    }

    private InputStream createRequest(CreatePublicationRequest request) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreatePublicationRequest>(JsonUtils.dtoObjectMapper)
            .withBody(request)
            .build();
    }
}
