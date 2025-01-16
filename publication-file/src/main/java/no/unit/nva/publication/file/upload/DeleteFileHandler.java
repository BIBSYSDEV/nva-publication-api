package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class DeleteFileHandler extends ApiGatewayHandler<Void, Void> {

    private final ResourceService resourceService;

    @JacocoGenerated
    private DeleteFileHandler() {
        this(ResourceService.defaultService());
    }

    public DeleteFileHandler(ResourceService resourceService) {
        super(Void.class);
        this.resourceService = resourceService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {

    }

    @Override
    protected Void processInput(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, Void o) {
        return HTTP_ACCEPTED;
    }
}
