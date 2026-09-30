package no.unit.nva.publication.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import org.junit.jupiter.api.Test;

class ApiGatewayProxyRequestBuilderTest {

    private static final String PATH = "/";
    private static final String CLAIMS_FIELD = "claims";
    private static final String USERNAME_CLAIM = "custom:nvaUsername";
    private static final String USERNAME = "testuser@sikt.no";

    @Test
    void shouldOmitAuthorizerSoThatNvaCommonsValidatesTheTokenItself() throws ApiIoException {
        var proxyRequest = buildRequestWith(context -> Optional.empty());

        assertThat(RequestInfo.fromString(proxyRequest).isGatewayAuthorized(), is(false));
    }

    @Test
    void shouldKeepAuthorizerWhenProviderSuppliesClaims() throws ApiIoException {
        var proxyRequest = buildRequestWith(context -> Optional.of(authorizerNode()));

        assertThat(RequestInfo.fromString(proxyRequest).isGatewayAuthorized(), is(true));
    }

    private static String buildRequestWith(AuthorizerContextProvider provider) {
        var builder = new ApiGatewayProxyRequestBuilder(JsonUtils.dtoObjectMapper, provider);
        return builder.build(fakeContext(), Map.of());
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode authorizerNode() {
        var authorizer = JsonUtils.dtoObjectMapper.createObjectNode();
        authorizer.putObject(CLAIMS_FIELD).put(USERNAME_CLAIM, USERNAME);
        return authorizer;
    }

    private static Context fakeContext() {
        var context = mock(Context.class);
        when(context.path()).thenReturn(PATH);
        when(context.method()).thenReturn(HandlerType.GET);
        when(context.headerMap()).thenReturn(Map.of());
        when(context.queryParamMap()).thenReturn(Map.<String, List<String>>of());
        when(context.body()).thenReturn("");
        return context;
    }
}
