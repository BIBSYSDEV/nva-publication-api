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
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import no.unit.nva.publication.file.upload.restmodel.ListPartsElement;
import no.unit.nva.publication.file.upload.restmodel.ListPartsRequestBody;
import no.unit.nva.publication.file.upload.restmodel.ListPartsResponseBody;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class ListPartsHandlerTest {

    public static final int SAMPLE_PART_NUMBER = 1;
    public static final String SAMPLE_ETAG = "eTag";
    public static final int SAMPLE_SIZE = 1;
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_KEY = "key";
    private ListPartsHandler listPartsHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    @BeforeEach
    void setUp() {
        s3client = mock(AmazonS3Client.class);
        listPartsHandler = new ListPartsHandler(s3client);
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
        when(s3client.listParts(any(ListPartsRequest.class))).thenThrow(AmazonS3Exception.class);
        listPartsHandler.handleRequest(listPartsRequestWithBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void canCreateListPartsElementFromPartSummary() {
        var partSummary = new PartSummary();
        partSummary.setPartNumber(SAMPLE_PART_NUMBER);
        partSummary.setETag(SAMPLE_ETAG);
        partSummary.setSize(SAMPLE_SIZE);

        var listPartsElement = ListPartsElement.create(partSummary);

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

    private PartListing listPartsResponse() {
        var partSummary1 = new PartSummary();
        partSummary1.setPartNumber(1);
        partSummary1.setETag("ETag1");
        partSummary1.setSize(1);
        var partsSummary = new ArrayList<PartSummary>();
        partsSummary.add(partSummary1);

        var partSummary2 = new PartSummary();
        partSummary2.setPartNumber(2);
        partSummary2.setETag("ETag2");
        partSummary2.setSize(2);
        partsSummary.add(partSummary2);

        var listPartsResponse = new PartListing();
        listPartsResponse.setParts(partsSummary);
        return listPartsResponse;
    }

    private PartListing truncatedPartListing() {
        var partListing = mock(PartListing.class);
        when(partListing.getParts()).thenReturn(listPartsResponse().getParts());
        when(partListing.isTruncated()).thenReturn(true).thenReturn(false);
        return partListing;
    }
}
