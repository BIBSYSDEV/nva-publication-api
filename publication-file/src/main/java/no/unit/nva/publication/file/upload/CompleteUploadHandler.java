package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadRequest;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CompleteUploadHandler extends ApiGatewayHandler<CompleteUploadRequest, File> {

    private final FileService fileService;
    private final IdentityServiceClient identityServiceClient;

    @JacocoGenerated
    public CompleteUploadHandler() {
        this(FileService.defaultFileService(), IdentityServiceClient.prepare(), new Environment());
    }

    public CompleteUploadHandler(FileService fileService, IdentityServiceClient identityServiceClient,
                                 Environment environment) {
        super(CompleteUploadRequest.class, environment);
        this.fileService = fileService;
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected void validateRequest(CompleteUploadRequest completeUploadRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        completeUploadRequestBody.validate();
    }

    @Override
    protected File processInput(CompleteUploadRequest input, RequestInfo requestInfo,
                                Context context) throws ApiGatewayException {

        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        return fileService.completeMultipartUpload(resourceIdentifier, input, userInstance);
    }

    @Override
    protected Integer getSuccessStatusCode(CompleteUploadRequest input, File output) {
        return HTTP_OK;
    }
}