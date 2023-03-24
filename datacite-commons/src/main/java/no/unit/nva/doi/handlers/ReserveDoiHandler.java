package no.unit.nva.doi.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.doi.service.ReserveDoiService;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public class ReserveDoiHandler extends ApiGatewayHandler<Void, DoiResponse> {

    public static final String BAD_RESPONSE_ERROR_MESSAGE = "Bad response from DataCite";
    private final ReserveDoiService reserveDoiService;

    @JacocoGenerated
    public ReserveDoiHandler() {
        this(ResourceService.defaultService(),
             new DataCiteDoiClient(getDefaultHttpClient(),
                                   SecretsReader.defaultSecretsManagerClient(), new Environment().readEnv("API_HOST")),
             new Environment());
    }

    public ReserveDoiHandler(ResourceService resourceService,
                             DataCiteDoiClient reserveDoiClient, Environment environment) {
        super(Void.class, environment);
        this.reserveDoiService = new ReserveDoiService(resourceService, reserveDoiClient);
    }

    @Override
    protected DoiResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var owner = RequestUtil.getOwner(requestInfo);
        var publicationIdentifier = RequestUtil.getIdentifier(requestInfo);
        return reserveDoiService.reserve(owner, publicationIdentifier);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, DoiResponse output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @JacocoGenerated
    private static HttpClient getDefaultHttpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }
}
