package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import no.unit.nva.publication.file.upload.restmodel.AbortMultipartUploadRequestBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class AbortMultipartUploadHandler extends ApiGatewayHandler<AbortMultipartUploadRequestBody, Void> {

    private final AmazonS3 s3Client;

    @JacocoGenerated
    public AbortMultipartUploadHandler() {
        this(AmazonS3ClientBuilder.defaultClient(), new Environment());
    }

    public AbortMultipartUploadHandler(AmazonS3 s3Client, Environment environment) {
        super(AbortMultipartUploadRequestBody.class, environment);
        this.s3Client = s3Client;
    }

    @Override
    protected void validateRequest(AbortMultipartUploadRequestBody abortMultipartUploadRequestBody,
                                   RequestInfo requestInfo, Context context) throws ApiGatewayException {
        abortMultipartUploadRequestBody.validate();
    }

    @Override
    protected Void processInput(AbortMultipartUploadRequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var request = input.toAbortMultipartUploadRequest(BUCKET_NAME);
        s3Client.abortMultipartUpload(request);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(AbortMultipartUploadRequestBody input, Void output) {
        return HTTP_ACCEPTED;
    }
}

