package no.unit.nva.publication.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.Context;
import java.util.Map;

public final class ApiGatewayProxyRequestBuilder {

    private static final String HEADERS = "headers";
    private static final String PATH = "path";
    private static final String PATH_PARAMETERS = "pathParameters";
    private static final String QUERY_STRING_PARAMETERS = "queryStringParameters";
    private static final String MULTI_VALUE_QUERY_STRING_PARAMETERS = "multiValueQueryStringParameters";
    private static final String REQUEST_CONTEXT = "requestContext";
    private static final String HTTP_METHOD = "httpMethod";
    private static final String BODY = "body";
    private static final String IS_BASE64_ENCODED = "isBase64Encoded";
    private static final String AUTHORIZER = "authorizer";

    private final ObjectMapper objectMapper;
    private final AuthorizerContextProvider authorizerContextProvider;

    public ApiGatewayProxyRequestBuilder(ObjectMapper objectMapper,
                                         AuthorizerContextProvider authorizerContextProvider) {
        this.objectMapper = objectMapper;
        this.authorizerContextProvider = authorizerContextProvider;
    }

    public String build(Context ctx, Map<String, String> pathParameters) {
        var node = objectMapper.createObjectNode();
        node.put(PATH, ctx.path());
        node.put(HTTP_METHOD, ctx.method().name());
        node.put(IS_BASE64_ENCODED, false);
        addHeaders(ctx, node);
        addPathParameters(pathParameters, node);
        addQueryParameters(ctx, node);
        addBody(ctx, node);
        addRequestContext(ctx, node);
        return node.toString();
    }

    private void addHeaders(Context ctx, ObjectNode node) {
        var headers = node.putObject(HEADERS);
        ctx.headerMap().forEach(headers::put);
    }

    private static void addPathParameters(Map<String, String> pathParameters, ObjectNode node) {
        var pathParams = node.putObject(PATH_PARAMETERS);
        pathParameters.forEach(pathParams::put);
    }

    private static void addQueryParameters(Context ctx, ObjectNode node) {
        var queryParams = node.putObject(QUERY_STRING_PARAMETERS);
        var multiQueryParams = node.putObject(MULTI_VALUE_QUERY_STRING_PARAMETERS);
        ctx.queryParamMap().forEach((name, values) -> {
            if (!values.isEmpty()) {
                queryParams.put(name, values.get(0));
                ArrayNode array = multiQueryParams.putArray(name);
                values.forEach(array::add);
            }
        });
    }

    private static void addBody(Context ctx, ObjectNode node) {
        var body = ctx.body();
        if (body.isEmpty()) {
            node.putNull(BODY);
        } else {
            node.put(BODY, body);
        }
    }

    private void addRequestContext(Context ctx, ObjectNode node) {
        var requestContext = node.putObject(REQUEST_CONTEXT);
        authorizerContextProvider.buildAuthorizerNode(ctx)
            .ifPresentOrElse(
                authorizerNode -> requestContext.set(AUTHORIZER, authorizerNode),
                () -> requestContext.putObject(AUTHORIZER));
    }
}
