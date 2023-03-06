package no.unit.nva.doi.handlers;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import no.unit.nva.doi.DataCiteReserveDoiClient;
import no.unit.nva.doi.ReserveDoiRequestValidator;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ReserveDoiHandler extends ApiGatewayHandler<Void, DoiResponse> {


    public static final String BAD_RESPONSE_ERROR_MESSAGE = "Bad response from DataCite";
    private final ResourceService resourceService;
    private final DataCiteReserveDoiClient reserveDoiClient;

    @JacocoGenerated
    public ReserveDoiHandler() {
        this(ResourceService.defaultService(),
             new DataCiteReserveDoiClient(getDefaultHttpClient(), new Environment()), new Environment());
    }

    public ReserveDoiHandler(ResourceService resourceService,
                             DataCiteReserveDoiClient reserveDoiClient, Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.reserveDoiClient = reserveDoiClient;
    }

    @Override
    protected DoiResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publication = fetchPublication(requestInfo);
        ReserveDoiRequestValidator.validateRequest(requestInfo, publication);
        return attempt(() -> reserveDoiClient.generateDoi(publication))
                   .map(doi -> updatePublicationWithDoi(publication, doi))
                   .orElseThrow(failure -> new BadGatewayException(BAD_RESPONSE_ERROR_MESSAGE));
    }

    private DoiResponse updatePublicationWithDoi(Publication publication, URI doi) {
        var updatedPublication = publication.copy()
                                     .withDoi(doi)
                                     .build();
        resourceService.updatePublication(updatedPublication);
        return new DoiResponse(doi);
    }

    private Publication fetchPublication(RequestInfo requestInfo) throws ApiGatewayException {
        return resourceService.getPublicationByIdentifier(RequestUtil.getIdentifier(requestInfo));
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
