package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

import java.net.HttpURLConnection;
import java.util.List;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;

public class FetchPublicationContextHandler extends ApiGatewayHandler<Void, String> {

    private static final String ENV_API_HOST = "API_HOST";
    private static final String ENV_CUSTOM_DOMAIN_BASE_PATH = "CUSTOM_DOMAIN_BASE_PATH";

    public FetchPublicationContextHandler() {
        super(Void.class, new Environment());
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var host = environment.readEnv(ENV_API_HOST);
        var path = environment.readEnv(ENV_CUSTOM_DOMAIN_BASE_PATH);
        var baseUri = UriWrapper.fromHost(host)
                .addChild(path)
                .getUri();
        return Publication.getJsonLdContext(baseUri);
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

}
