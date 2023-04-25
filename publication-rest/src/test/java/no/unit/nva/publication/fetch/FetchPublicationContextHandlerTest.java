package no.unit.nva.publication.fetch;

import com.fasterxml.jackson.core.JsonProcessingException;
import static com.google.common.net.HttpHeaders.ACCEPT;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.stream.Stream;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;

class FetchPublicationContextHandlerTest {

    private static final String TEXT_ANY = "text/*";
    private static final String TEXT_HTML = "text/html";
    private static final String APPLICATION_XHTML = "application/xhtml+xml";
    private static final String APPLICATION_JSON_LD = "application/ld+json";
    private static final String APPLICATION_JSON = "application/json";
    private static final String DEFAULT_MEDIA_TYPE = "*/*";
    private static final String UNSUPPORTED_ACCEPT_HEADER_MESSAGE = "contains no supported Accept header values.";
    private FetchPublicationContextHandler fetchPublicationContextHandler;
    private Context context;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        fetchPublicationContextHandler = new FetchPublicationContextHandler();
    }

    @ParameterizedTest(name = "mediaType {0} is invalid")
    @MethodSource("unsupportedMediaTypes")
    void shouldReturnUnsupportedMediaTypeIfRequestHeaderAcceptsAnythingOtherThanJsonOrJsonLdOrDefault(String mediaType)
            throws IOException {
        var request = generateHandlerRequest(Map.of(ACCEPT, mediaType));
        fetchPublicationContextHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)));
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(containsString(UNSUPPORTED_ACCEPT_HEADER_MESSAGE)));
    }

    @ParameterizedTest(name = "mediaType {0} is valid")
    @MethodSource("supportedMediaTypes")
    void shouldReturnPublicationContextIfRequestHeaderAcceptsJsonOrJsonLdOrDefault(String mediaType)
            throws IOException {
        var request = generateHandlerRequest(Map.of(ACCEPT, mediaType));
        fetchPublicationContextHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    private InputStream generateHandlerRequest(Map<String, String> headers)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                .withHeaders(headers)
                .build();
    }

    private static Stream<String> unsupportedMediaTypes() {
        return Stream.of(TEXT_ANY, TEXT_HTML, APPLICATION_XHTML);
    }

    private static Stream<String> supportedMediaTypes() {
        return Stream.of(APPLICATION_JSON, APPLICATION_JSON_LD, DEFAULT_MEDIA_TYPE);
    }
}
