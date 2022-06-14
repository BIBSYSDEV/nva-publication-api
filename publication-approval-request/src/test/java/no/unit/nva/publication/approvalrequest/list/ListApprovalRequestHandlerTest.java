package no.unit.nva.publication.approvalrequest.list;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListApprovalRequestHandlerTest {

    private ListApprovalRequestHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;

    @BeforeEach
    public void initialize() {
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        handler = new ListApprovalRequestHandler(new Environment());
    }

    @Test
    public void listDummyApprovalWithRandomStringReturnsOk() throws IOException {
        handler.handleRequest(createRequest(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_OK));
    }

    private InputStream createRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper).build();
    }

}
