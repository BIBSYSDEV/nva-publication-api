package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UriWrapper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;

public class FetchPublicationContextHandler extends ApiGatewayHandler<Void, String> {

    private static final String HOST = ENVIRONMENT.readEnv("API_HOST");
    private static final String PATH = ENVIRONMENT.readEnv("CUSTOM_DOMAIN_BASE_PATH");
    private static final URI BASE_URI = UriWrapper.fromHost(HOST).addChild(PATH).getUri();

    public FetchPublicationContextHandler() {
        super(Void.class);
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return Publication.getJsonLdContext(BASE_URI);
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
