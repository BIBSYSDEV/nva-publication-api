package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadRequestBody;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class CompleteUploadHandler extends ApiGatewayHandler<CompleteUploadRequestBody, UploadedFile> {

    private final FileService fileService;

    @JacocoGenerated
    public CompleteUploadHandler() {
        this(FileService.defaultFileService());
    }

    public CompleteUploadHandler(FileService fileService) {
        super(CompleteUploadRequestBody.class);
        this.fileService = fileService;
    }

    @Override
    protected void validateRequest(CompleteUploadRequestBody completeUploadRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        completeUploadRequestBody.validate();
    }

    @Override
    protected UploadedFile processInput(CompleteUploadRequestBody input, RequestInfo requestInfo,
                                        Context context) throws ApiGatewayException {

        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);

        return fileService.completeMultipartUpload(resourceIdentifier, input, userInstance);
    }

    @Override
    protected Integer getSuccessStatusCode(CompleteUploadRequestBody input, UploadedFile output) {
        return HTTP_OK;
    }
}