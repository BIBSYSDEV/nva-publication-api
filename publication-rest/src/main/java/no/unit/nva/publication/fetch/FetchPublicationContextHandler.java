package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;

public class FetchPublicationContextHandler extends ApiGatewayHandler<Void, String> {

    private static final String HOST = ENVIRONMENT.readEnv("API_HOST");
    private static final String PATH = ENVIRONMENT.readEnv("CUSTOM_DOMAIN_BASE_PATH");
    private static final URI BASE_URI = UriWrapper.fromHost(HOST).addChild(PATH).getUri();
    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
    public static final String CONTEXT_PROPERTY = "@context";

    @JacocoGenerated
    public FetchPublicationContextHandler() {
        this(new Environment());
    }

    public FetchPublicationContextHandler(Environment environment) {
        super(Void.class, environment);
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return generateContextString();
    }

    private static String generateContextString() {
        var contextNode = MAPPER.createObjectNode();
        var contextValue = attempt(() -> MAPPER.readTree(Publication.getJsonLdContext(BASE_URI))).orElseThrow();
        contextNode.set(CONTEXT_PROPERTY, contextValue);
        return attempt(() -> MAPPER.writeValueAsString(contextNode)).orElseThrow();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(
                APPLICATION_JSON_LD,
                JSON_UTF_8
        );
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }
}
