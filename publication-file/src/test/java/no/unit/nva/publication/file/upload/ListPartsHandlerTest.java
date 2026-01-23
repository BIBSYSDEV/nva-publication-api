package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import no.unit.nva.publication.file.upload.restmodel.ListPartsElement;
import no.unit.nva.publication.file.upload.restmodel.ListPartsRequestBody;
import no.unit.nva.publication.file.upload.restmodel.ListPartsResponseBody;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class ListPartsHandlerTest {

    public static final int SAMPLE_PART_NUMBER = 1;
    public static final String SAMPLE_ETAG = "eTag";
    public static final int SAMPLE_SIZE = 1;
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_KEY = "key";
    private ListPartsHandler listPartsHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private S3Client s3client;

    @BeforeEach
    void setUp() {
        s3client = mock(S3Client.class);
        listPartsHandler = new ListPartsHandler(s3client, new Environment());
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void canListParts() throws IOException {
        when(s3client.listParts(any(ListPartsRequest.class))).thenReturn(listPartsResponse());
        listPartsHandler.handleRequest(listPartsRequestWithBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, ListPartsResponseBody.class);
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(response.getBody(), is(notNullValue()));
        var responseBody = response.getBodyObject(ListPartsResponseBody.class);
        assertThat(responseBody, is(notNullValue()));
    }

    @Test
    void canListPartsWhenManyParts() throws IOException {
        var partListing = truncatedPartListing();
        when(s3client.listParts(any(ListPartsRequest.class))).thenReturn(partListing);
        listPartsHandler.handleRequest(listPartsRequestWithBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, ListPartsResponseBody.class);
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(response.getBody(), is(notNullValue()));
        var responseBody = response.getBodyObject(ListPartsResponseBody.class);
        assertThat(responseBody, is(notNullValue()));
    }

    @Test
    void listPartsWithInvalidInputReturnsBadRequest() throws IOException {
        listPartsHandler.handleRequest(listPartsRequestWithoutBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void listPartsWithS3ErrorReturnsInternalServerError() throws IOException {
        when(s3client.listParts(any(ListPartsRequest.class))).thenThrow(S3Exception.class);
        listPartsHandler.handleRequest(listPartsRequestWithBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void canCreateListPartsElementFromPart() {
        var part = Part.builder()
                       .partNumber(SAMPLE_PART_NUMBER)
                       .eTag(SAMPLE_ETAG)
                       .size((long) SAMPLE_SIZE)
                       .build();

        var listPartsElement = ListPartsElement.create(part);

        assertThat(listPartsElement.etag(), is(equalTo(SAMPLE_ETAG)));
        assertThat(listPartsElement.partNumber(), is(equalTo(Integer.toString(SAMPLE_PART_NUMBER))));
        assertThat(listPartsElement.size(), is(equalTo(Integer.toString(SAMPLE_SIZE))));

        var listParts = new ListPartsElement(Integer.toString(SAMPLE_PART_NUMBER), Integer.toString(SAMPLE_SIZE),
                                             SAMPLE_ETAG);

        assertThat(listParts.etag(), is(equalTo(SAMPLE_ETAG)));
        assertThat(listParts.partNumber(), is(equalTo(Integer.toString(SAMPLE_PART_NUMBER))));
        assertThat(listParts.size(), is(equalTo(Integer.toString(SAMPLE_SIZE))));
    }

    private InputStream listPartsRequestWithoutBody() throws JsonProcessingException {
        return new HandlerRequestBuilder<ListPartsRequestBody>(dtoObjectMapper).withBody(
            new ListPartsRequestBody(null, null)).build();
    }

    private InputStream listPartsRequestWithBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<ListPartsRequestBody>(dtoObjectMapper).withBody(listPartsRequestBody())
                   .build();
    }

    private ListPartsRequestBody listPartsRequestBody() {
        return new ListPartsRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
    }

    private ListPartsResponse listPartsResponse() {
        var part1 = Part.builder()
                        .partNumber(1)
                        .eTag("ETag1")
                        .size(1L)
                        .build();

        var part2 = Part.builder()
                        .partNumber(2)
                        .eTag("ETag2")
                        .size(2L)
                        .build();

        return ListPartsResponse.builder()
                   .parts(List.of(part1, part2))
                   .isTruncated(false)
                   .build();
    }

    private ListPartsResponse truncatedPartListing() {
        var partListing = mock(ListPartsResponse.class);
        when(partListing.parts()).thenReturn(listPartsResponse().parts());
        when(partListing.isTruncated()).thenReturn(true).thenReturn(false);
        return partListing;
    }
}
