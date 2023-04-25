package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.List;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

public class FetchPublicationContextHandler extends ApiGatewayHandler<Void, String> {

    public FetchPublicationContextHandler() {
        super(Void.class);
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return stringFromResources(Path.of("publicationContext.json"));
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
