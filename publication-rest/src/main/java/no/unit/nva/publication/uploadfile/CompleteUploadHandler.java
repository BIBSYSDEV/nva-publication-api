package no.unit.nva.publication.uploadfile;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.uploadfile.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import no.unit.nva.publication.uploadfile.restmodel.CompleteUploadRequestBody;
import no.unit.nva.publication.uploadfile.restmodel.CompleteUploadResponseBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class CompleteUploadHandler extends ApiGatewayHandler<CompleteUploadRequestBody, CompleteUploadResponseBody> {

    private final AmazonS3 amazonS3;

    @JacocoGenerated
    public CompleteUploadHandler() {
        this(AmazonS3ClientBuilder.defaultClient());
    }

    public CompleteUploadHandler(AmazonS3 amazonS3) {
        super(CompleteUploadRequestBody.class);
        this.amazonS3 = amazonS3;
    }

    @Override
    protected void validateRequest(CompleteUploadRequestBody completeUploadRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        completeUploadRequestBody.validate();
    }

    @Override
    protected CompleteUploadResponseBody processInput(CompleteUploadRequestBody input, RequestInfo requestInfo,
                                                      Context context) throws ApiGatewayException {

        var request = input.toCompleteMultipartUploadRequest(BUCKET_NAME);
        var result = amazonS3.completeMultipartUpload(request);
        var objectMetadata = amazonS3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, result.getKey()));

        return CompleteUploadResponseBody.create(objectMetadata, result.getKey());
    }

    @Override
    protected Integer getSuccessStatusCode(CompleteUploadRequestBody input, CompleteUploadResponseBody output) {
        return HTTP_OK;
    }
}