package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadPart;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadRequest;
import no.unit.nva.publication.file.upload.restmodel.ExternalCompleteUploadRequest;
import no.unit.nva.publication.file.upload.restmodel.InternalCompleteUploadRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class CompleteUploadHandlerTest extends ResourcesLocalTest {

    public static final String SAMPLE_KEY = "key";
    public static final String SAMPLE_UPLOAD_ID = "uploadID";
    public static final int EXPECTED_ONE_PART = 1;
    private static final String COMPLETE_UPLOAD_REQUEST_WITH_EMPTY_ELEMENT_JSON =
        "CompleteRequestWithEmptyElement.json";
    private static final String COMPLETE_UPLOAD_REQUEST_WITH_ONE_PART_JSON = "CompleteRequestWithOnePart.json";
    private CompleteUploadHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;
    private ResourceService resourceService;

    /**
     * Setup test env.
     */
    @BeforeEach
    void setUp() {
        super.init();
        s3client = mock(AmazonS3Client.class);
        resourceService = getResourceServiceBuilder().build();
        handler = new CompleteUploadHandler(new FileService(s3client, mock(CustomerApiClient.class), resourceService));
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @ParameterizedTest
    @ValueSource(strings = {"filename=\"filename.pdf\"", "", "filename=\"\"", "filename.pdf",
        "filename=\"Screenshot 2023-08-17 at 19.18.56.png\""})
    void canCompleteUpload(String filename) throws IOException, BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        mockS3(filename);
        handler.handleRequest(request(resource.getIdentifier(), userInstance), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, UploadedFile.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void completeUploadWithInvalidInputReturnsBadRequestWhenInternalClient() throws IOException {
        handler.handleRequest(completeInternalUploadRequestWithoutBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void completeUploadWithInvalidInputReturnsBadRequestWhenExternalClient() throws IOException {
        handler.handleRequest(completeExternalUploadRequestWithoutBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void completeUploadWithS3ErrorReturnsInternalServerError() throws IOException, BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        when(s3client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
            .thenThrow(AmazonS3Exception.class);

        handler.handleRequest(request(resource.getIdentifier(), userInstance), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void canCreateRequestWithEmptyElement() throws IOException {
        var stream = inputStreamFromResources(COMPLETE_UPLOAD_REQUEST_WITH_EMPTY_ELEMENT_JSON);
        var completeUploadRequestBody = dtoObjectMapper.readValue(
            new InputStreamReader(stream), InternalCompleteUploadRequest.class);

        var completeMultipartUploadRequest =
            completeUploadRequestBody.toCompleteMultipartUploadRequest(randomString());

        assertThat(completeUploadRequestBody, is(notNullValue()));
        assertThat(completeUploadRequestBody.parts(),
                   not(hasSize(completeMultipartUploadRequest.getPartETags().size())));
        assertThat(completeMultipartUploadRequest, is(notNullValue()));
    }

    @Test
    void canCreateRequestWithOnePart() throws IOException {
        var stream = inputStreamFromResources(COMPLETE_UPLOAD_REQUEST_WITH_ONE_PART_JSON);
        var completeUploadRequestBody = dtoObjectMapper.readValue(
            new InputStreamReader(stream), InternalCompleteUploadRequest.class);

        assertThat(completeUploadRequestBody, is(notNullValue()));
        assertThat(completeUploadRequestBody.parts(), is(notNullValue()));
        assertThat(completeUploadRequestBody.parts(), hasSize(EXPECTED_ONE_PART));

        var completeMultipartUploadRequest = completeUploadRequestBody.toCompleteMultipartUploadRequest(randomString());

        assertThat(completeMultipartUploadRequest, is(notNullValue()));
        assertThat(completeUploadRequestBody.parts(), hasSize(completeMultipartUploadRequest.getPartETags().size()));
    }

    private void mockS3(String filename) {
        var completeMultipartUploadResult = new CompleteMultipartUploadResult();
        completeMultipartUploadResult.setKey(UUID.randomUUID().toString());
        when(s3client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class))).thenReturn(
            completeMultipartUploadResult);
        var s3object = new S3Object();
        s3object.setKey(randomString());
        var metadata = new ObjectMetadata();
        metadata.setContentLength(12345);
        metadata.setContentDisposition(filename);
        metadata.setContentType("application/pdf");
        s3object.setObjectMetadata(metadata);
        when(s3client.getObjectMetadata(any())).thenReturn(metadata);
    }

    private InputStream request(SortableIdentifier identifier, UserInstance userInstance) throws JsonProcessingException {
        return new HandlerRequestBuilder<InternalCompleteUploadRequest>(dtoObjectMapper)
                   .withBody(completeUploadRequestBody())
                   .withPathParameters(Map.of("publicationIdentifier", identifier.toString()))
                   .withUserName(userInstance.getUsername())
                   .withCurrentCustomer(userInstance.getCustomerId())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream completeExternalUploadRequestWithoutBody() throws JsonProcessingException {
        return new HandlerRequestBuilder<CompleteUploadRequest>(dtoObjectMapper)
                   .withBody(new ExternalCompleteUploadRequest(null, null, null, null, null, null, null))
                   .build();
    }

    private InputStream completeInternalUploadRequestWithoutBody() throws JsonProcessingException {
        return new HandlerRequestBuilder<CompleteUploadRequest>(dtoObjectMapper)
                   .withBody(new InternalCompleteUploadRequest(null, null, null))
                   .build();
    }

    private InternalCompleteUploadRequest completeUploadRequestBody() {
        var partEtags = new ArrayList<CompleteUploadPart>();
        partEtags.add(new CompleteUploadPart(1, "eTag1"));
        return new InternalCompleteUploadRequest(SAMPLE_UPLOAD_ID, SAMPLE_KEY, partEtags);
    }
}
