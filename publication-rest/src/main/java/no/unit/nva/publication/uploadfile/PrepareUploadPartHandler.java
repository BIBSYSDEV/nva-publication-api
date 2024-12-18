package no.unit.nva.publication.uploadfile;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import no.unit.nva.publication.uploadfile.restmodel.PrepareUploadPartRequestBody;
import no.unit.nva.publication.uploadfile.restmodel.PrepareUploadPartResponseBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class PrepareUploadPartHandler
    extends ApiGatewayHandler<PrepareUploadPartRequestBody, PrepareUploadPartResponseBody> {

    public static final String BUCKET_NAME = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private final AmazonS3 amazonS3;

    @JacocoGenerated
    public PrepareUploadPartHandler() {
        this(AmazonS3ClientBuilder.defaultClient());
    }

    public PrepareUploadPartHandler(AmazonS3 amazonS3) {
        super(PrepareUploadPartRequestBody.class);
        this.amazonS3 = amazonS3;
    }

    @Override
    protected void validateRequest(PrepareUploadPartRequestBody prepareUploadPartRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        prepareUploadPartRequestBody.validate();
    }

    @Override
    protected PrepareUploadPartResponseBody processInput(PrepareUploadPartRequestBody input, RequestInfo requestInfo,
                                                         Context context) throws ApiGatewayException {

        var request = input.toGeneratePresignedUrlRequest(BUCKET_NAME);
        var url = amazonS3.generatePresignedUrl(request);

        return new PrepareUploadPartResponseBody(url);
    }

    @Override
    protected Integer getSuccessStatusCode(PrepareUploadPartRequestBody input, PrepareUploadPartResponseBody output) {
        return HTTP_OK;
    }
}
