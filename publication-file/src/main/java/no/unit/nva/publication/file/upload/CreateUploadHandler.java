package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadResponseBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class CreateUploadHandler extends ApiGatewayHandler<CreateUploadRequestBody, CreateUploadResponseBody> {

    private final AmazonS3 amazonS3;

    @JacocoGenerated
    public CreateUploadHandler() {
        this(AmazonS3ClientBuilder.defaultClient());
    }

    public CreateUploadHandler(AmazonS3 amazonS3) {
        super(CreateUploadRequestBody.class);
        this.amazonS3 = amazonS3;
    }

    @Override
    protected void validateRequest(CreateUploadRequestBody createUploadRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        createUploadRequestBody.validate();
    }

    @Override
    protected CreateUploadResponseBody processInput(CreateUploadRequestBody input, RequestInfo requestInfo,
                                                    Context context) throws ApiGatewayException {

        var request = input.toInitiateMultipartUploadRequest(BUCKET_NAME);
        var result = amazonS3.initiateMultipartUpload(request);

        return CreateUploadResponseBody.fromInitiateMultipartUploadResult(result);
    }

    @Override
    protected Integer getSuccessStatusCode(CreateUploadRequestBody input, CreateUploadResponseBody output) {
        return HTTP_OK;
    }
}