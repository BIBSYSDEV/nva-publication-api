package no.unit.nva.publication.download;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.testutils.TestHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.testutils.TestHeaders.APPLICATION_PROBLEM_JSON;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpHeaders.LOCATION;
import static org.apache.http.HttpStatus.SC_MOVED_PERMANENTLY;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.services.UriResolver;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class ResolveShortenedUrlHandlerTest {

    private static final Context CONTEXT = mock(Context.class);
    private ResolveShortenedUrlHandler handler;
    private ByteArrayOutputStream output;
    private static final String ANY_ORIGIN = "*";
    private static final String APPLICATION_JSON = "application/json; charset=utf-8";
    private UriResolver resolver;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        resolver = mock(UriResolver.class);
    }

    @Test
    void shouldReturnNotFoundWhenUriResolverReturnsNotFound() throws IOException, ApiGatewayException {
        handler = new ResolveShortenedUrlHandler(mockEnvironment(), resolver, mock(HttpClient.class));
        when(resolver.resolve(any())).thenThrow(new NotFoundException("Not found"));
        handler.handleRequest(
            createRequest(),
            output,
            CONTEXT);
        var gatewayResponse = GatewayResponse.fromString(output.toString(), Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    @Test
    void shouldReturnRedirectWhenUriResolverReturnsLongUri() throws IOException, ApiGatewayException {
        var expectedUri = randomUri();
        handler = new ResolveShortenedUrlHandler(mockEnvironment(), resolver, mock(HttpClient.class));
        when(resolver.resolve(any())).thenReturn(expectedUri);
        handler.handleRequest(
            createRequest(),
            output,
            CONTEXT);
        var gatewayResponse = GatewayResponse.fromString(output.toString(), Void.class);
        assertBasicRestRequirements(gatewayResponse, SC_MOVED_PERMANENTLY, APPLICATION_JSON);
        assertThat(gatewayResponse.getHeaders().get(LOCATION), equalTo(expectedUri.toString()));
    }

    private void assertBasicRestRequirements(GatewayResponse<?> gatewayResponse,
                                             int expectedStatusCode,
                                             String expectedContentType) {
        assertThat(gatewayResponse.getStatusCode(), equalTo(expectedStatusCode));
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders().get(CONTENT_TYPE), equalTo(expectedContentType));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getHeaders().get(ACCESS_CONTROL_ALLOW_ORIGIN), equalTo(ANY_ORIGIN));
    }

    private InputStream createRequest()
        throws IOException {
        var identifier = SortableIdentifier.next().toString();
        var domainNameNode = dtoObjectMapper.createObjectNode();
        domainNameNode.put("domainName", "www.example.com");
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withHeaders(TestHeaders.getRequestHeaders())
                   .withRequestContext(domainNameNode)
                   .withOtherProperties(Map.of("path", "download/short/" + identifier))
                   .withPathParameters(Map.of("identifier", identifier))
                   .build();
    }



    private Environment mockEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(ANY_ORIGIN);
        return environment;
    }
}
