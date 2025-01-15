package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadResponseBody;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class CreateUploadHandler extends ApiGatewayHandler<CreateUploadRequestBody, CreateUploadResponseBody> {

    private final FileService fileService;

    @JacocoGenerated
    public CreateUploadHandler() {
        this(FileService.defaultFileService());
    }

    @JacocoGenerated
    private CreateUploadHandler(FileService fileService) {
        super(CreateUploadRequestBody.class);
        this.fileService = fileService;
    }

    public CreateUploadHandler(AmazonS3 amazonS3, CustomerApiClient customerApiClient, ResourceService resourceService) {
        super(CreateUploadRequestBody.class);
        this.fileService = new FileService(amazonS3, customerApiClient, resourceService);
    }

    @Override
    protected void validateRequest(CreateUploadRequestBody createUploadRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        createUploadRequestBody.validate();
    }

    @Override
    protected CreateUploadResponseBody processInput(CreateUploadRequestBody input, RequestInfo requestInfo,
                                                    Context context) throws ApiGatewayException {
        var customerId = requestInfo.getCurrentCustomer();
        var resourceIdentifier = RequestUtil.getIdentifier(requestInfo);

        var createUploadResponseBody =  fileService.initiateMultipartUpload(resourceIdentifier, customerId, input);

        return CreateUploadResponseBody.fromInitiateMultipartUploadResult(createUploadResponseBody);
    }

    @Override
    protected Integer getSuccessStatusCode(CreateUploadRequestBody input, CreateUploadResponseBody output) {
        return HTTP_OK;
    }
}