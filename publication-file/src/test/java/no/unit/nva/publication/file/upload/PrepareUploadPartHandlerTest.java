package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import no.unit.nva.publication.file.upload.restmodel.PrepareUploadPartRequestBody;
import no.unit.nva.publication.file.upload.restmodel.PrepareUploadPartResponseBody;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class PrepareUploadPartHandlerTest {

    public static final String SAMPLE_KEY = "key";
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_BODY = "body";
    public static final String SAMPLE_PART_NUMBER = "1";
    private PrepareUploadPartHandler prepareUploadPartHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    @BeforeEach
    void setUp() {
        s3client = mock(AmazonS3Client.class);
        prepareUploadPartHandler = new PrepareUploadPartHandler(s3client, new Environment());
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void canPrepareUploadPart() throws IOException {
        var dummyUrl = URI.create("http://localhost").toURL();
        when(s3client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class))).thenReturn(dummyUrl);

        prepareUploadPartHandler.handleRequest(prepareUploadPartRequestWithBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, PrepareUploadPartResponseBody.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(response.getBody(), is(notNullValue()));
        var responseBody = response.getBodyObject(PrepareUploadPartResponseBody.class);
        assertThat(responseBody.url(), is(notNullValue()));
    }

    @Test
    void prepareUploadPartWithInvalidInputReturnsBadRequest() throws IOException {
        prepareUploadPartHandler.handleRequest(prepareUploadPartRequestWithoutBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void prepareUploadPartWithS3ErrorReturnsInternalServerError() throws IOException {
        when(s3client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
            .thenThrow(AmazonS3Exception.class);

        prepareUploadPartHandler.handleRequest(prepareUploadPartRequestWithBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    private InputStream prepareUploadPartRequestWithBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<PrepareUploadPartRequestBody>(dtoObjectMapper).withBody(
            prepareUploadPartRequestBody()).build();
    }

    private InputStream prepareUploadPartRequestWithoutBody() throws JsonProcessingException {
        return new HandlerRequestBuilder<PrepareUploadPartRequestBody>(dtoObjectMapper)
                   .withBody(new PrepareUploadPartRequestBody(null, null, null, null))
                   .build();
    }

    private PrepareUploadPartRequestBody prepareUploadPartRequestBody() {
        return new PrepareUploadPartRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY, SAMPLE_BODY, SAMPLE_PART_NUMBER);
    }
}
