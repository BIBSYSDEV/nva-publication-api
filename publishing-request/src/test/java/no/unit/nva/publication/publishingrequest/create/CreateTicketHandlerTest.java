package no.unit.nva.publication.publishingrequest.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateTicketHandlerTest {
    
    public static final FakeContext CONTEXT = new FakeContext();
    private CreateTicketHandler handler;
    private ByteArrayOutputStream output;
    
    @BeforeEach
    public void init(){
        this.handler = new CreateTicketHandler();
        this.output = new ByteArrayOutputStream();
        
    }
    
    @Test
    void shouldAcknowledgeReceiptOfOpeningTicketRequest() throws IOException {
        handler = new CreateTicketHandler();
        var input = createHttpTicketCreationRequest();
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }
    
    private InputStream createHttpTicketCreationRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(new TicketDto())
                   .build();
    }
}
