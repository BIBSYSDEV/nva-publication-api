package no.unit.nva.publication.delete;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.InputException;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import nva.commons.utils.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static java.util.Collections.singletonMap;
import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class DeletePublicationHandlerTest {

    public static final String IDENTIFIER = "identifier";
    public static final String WILDCARD = "*";
    private DeletePublicationHandler handler;
    private PublicationService publicationService;
    private Environment environment;
    private OutputStream outputStream;
    private Context context;
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_VOID_RESPONSE_TYPE = objectMapper
            .getTypeFactory()
            .constructParametricType(GatewayResponse.class, Void.class);
    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE = objectMapper
            .getTypeFactory()
            .constructParametricType(GatewayResponse.class, Problem.class);

    @BeforeEach
    public void setUp() {
        publicationService = Mockito.mock(PublicationService.class);
        environment = Mockito.mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        handler = new DeletePublicationHandler(publicationService, environment);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    void handleRequestReturnsAcceptedWhenServiceIsOk() throws IOException {
        UUID identifier = UUID.randomUUID();
        InputStream inputStream = new HandlerRequestBuilder<Publication>(objectMapper)
                .withHeaders(TestHeaders.getRequestHeaders())
                .withPathParameters(singletonMap(IDENTIFIER, identifier.toString()))
                .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Void> gatewayResponse = toGatewayResponse();
        assertEquals(HttpStatus.SC_ACCEPTED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsBadRequestOnServiceInError() throws IOException, ApiGatewayException {
        UUID identifier = UUID.randomUUID();
        InputStream inputStream = new HandlerRequestBuilder<Publication>(objectMapper)
                .withHeaders(TestHeaders.getRequestHeaders())
                .withPathParameters(singletonMap(IDENTIFIER, identifier.toString()))
                .build();
        doThrow(InputException.class).when(publicationService).markPublicationForDeletion(identifier);

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(HttpStatus.SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    private GatewayResponse<Void> toGatewayResponse() throws JsonProcessingException {
        return objectMapper.readValue(outputStream.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_VOID_RESPONSE_TYPE);
    }

    private GatewayResponse<Problem> toGatewayResponseProblem() throws JsonProcessingException {
        return objectMapper.readValue(outputStream.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE);
    }

}
