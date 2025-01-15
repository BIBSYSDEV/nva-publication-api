package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.file.upload.restmodel.UpdateFileRequest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class UpdateFileHandler extends ApiGatewayHandler<UpdateFileRequest, File> {

    private final ResourceService resourceService;

    @JacocoGenerated
    public UpdateFileHandler() {
        this(ResourceService.defaultService());
    }

    public UpdateFileHandler(ResourceService resourceService) {
        super(UpdateFileRequest.class);
        this.resourceService = resourceService;
    }

    @Override
    protected void validateRequest(UpdateFileRequest updateFileRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        // Ignore
    }

    @Override
    protected File processInput(UpdateFileRequest updateFileRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(UpdateFileRequest updateFileRequest, File o) {
        return HTTP_OK;
    }
}
