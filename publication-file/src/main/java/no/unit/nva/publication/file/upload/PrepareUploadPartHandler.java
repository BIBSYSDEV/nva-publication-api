package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Duration;
import no.unit.nva.publication.file.upload.restmodel.PrepareUploadPartRequestBody;
import no.unit.nva.publication.file.upload.restmodel.PrepareUploadPartResponseBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

public class PrepareUploadPartHandler
    extends ApiGatewayHandler<PrepareUploadPartRequestBody, PrepareUploadPartResponseBody> {

    private static final Duration PRESIGN_DURATION = Duration.ofHours(1);
    private final S3Presigner s3Presigner;

    @JacocoGenerated
    public PrepareUploadPartHandler() {
        this(S3Presigner.create(), new Environment());
    }

    public PrepareUploadPartHandler(S3Presigner s3Presigner, Environment environment) {
        super(PrepareUploadPartRequestBody.class, environment);
        this.s3Presigner = s3Presigner;
    }

    @Override
    protected void validateRequest(PrepareUploadPartRequestBody prepareUploadPartRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        prepareUploadPartRequestBody.validate();
    }

    @Override
    protected PrepareUploadPartResponseBody processInput(PrepareUploadPartRequestBody input, RequestInfo requestInfo,
                                                         Context context) throws ApiGatewayException {

        var uploadPartRequest = input.toUploadPartRequest(BUCKET_NAME);
        var presignRequest = UploadPartPresignRequest.builder()
                                 .signatureDuration(PRESIGN_DURATION)
                                 .uploadPartRequest(uploadPartRequest)
                                 .build();
        var presignedRequest = s3Presigner.presignUploadPart(presignRequest);

        return new PrepareUploadPartResponseBody(presignedRequest.url());
    }

    @Override
    protected Integer getSuccessStatusCode(PrepareUploadPartRequestBody input, PrepareUploadPartResponseBody output) {
        return HTTP_OK;
    }
}
