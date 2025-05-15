package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadResponseBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreateUploadHandler extends ApiGatewayHandler<CreateUploadRequestBody, CreateUploadResponseBody> {

    private final FileService fileService;
    private final IdentityServiceClient identityServiceClient;

    @JacocoGenerated
    public CreateUploadHandler() {
        this(FileService.defaultFileService(), IdentityServiceClient.prepare(), new Environment());
    }

    @JacocoGenerated
    public CreateUploadHandler(FileService fileService, IdentityServiceClient identityServiceClient,
                          Environment environment) {
        super(CreateUploadRequestBody.class, environment);
        this.fileService = fileService;
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected void validateRequest(CreateUploadRequestBody createUploadRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        createUploadRequestBody.validate();
    }

    @Override
    protected CreateUploadResponseBody processInput(CreateUploadRequestBody input, RequestInfo requestInfo,
                                                    Context context) throws ApiGatewayException {
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);

        var createUploadResponseBody =  fileService.initiateMultipartUpload(resourceIdentifier, userInstance, input);

        return CreateUploadResponseBody.fromInitiateMultipartUploadResult(createUploadResponseBody);
    }

    @Override
    protected Integer getSuccessStatusCode(CreateUploadRequestBody input, CreateUploadResponseBody output) {
        return HTTP_OK;
    }
}