package no.unit.nva.publication.uploadfile;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.publication.uploadfile.restmodel.AbortMultipartUploadRequestBody;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class AbortMultipartUploadHandlerTest {

    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_KEY = "key";
    private AbortMultipartUploadHandler abortMultipartUploadHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    @BeforeEach
    void setUp() {
        s3client = mock(AmazonS3Client.class);
        abortMultipartUploadHandler = new AbortMultipartUploadHandler(s3client);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void canAbortMultipartUpload() throws IOException {
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithBody(), outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }

    @Test
    void abortMultipartUploadWithInvalidInputReturnsBadRequest() throws IOException {
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithoutBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void abortMultipartUploadWithS3ErrorReturnsInternalServerError() throws IOException {
        doThrow(AmazonS3Exception.class).when(s3client).abortMultipartUpload(Mockito.any());
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithBody(), outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    private InputStream abortMultipartUploadRequestWithBody() throws JsonProcessingException {
        return new HandlerRequestBuilder<AbortMultipartUploadRequestBody>(dtoObjectMapper).withBody(
            abortMultipartUploadRequestBody()).build();
    }

    private InputStream abortMultipartUploadRequestWithoutBody()
        throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<AbortMultipartUploadRequestBody>(dtoObjectMapper).withBody(
            new AbortMultipartUploadRequestBody(null, null)).build();
    }

    private AbortMultipartUploadRequestBody abortMultipartUploadRequestBody() {
        return new AbortMultipartUploadRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
    }
}
