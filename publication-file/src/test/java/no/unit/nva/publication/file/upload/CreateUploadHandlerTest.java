package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadResponseBody;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class CreateUploadHandlerTest {

    public static final String SAMPLE_FILENAME = "filename";
    public static final String SAMPLE_MIMETYPE = "mime/type";
    public static final String SAMPLE_SIZE_STRING = "222";
    public static final String SAMPLE_UPLOAD_KEY = "uploadKey";
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_UNICODE_FILENAME = "normal_üñīḉøđḝ_ƒıļæ_ňåɱë";
    public static final String EXPECTED_ESCAPED_FILENAME = "normal_\\u00FC\\u00F1\\u012B\\u1E09\\u00F8\\u0111\\u1E1D_" +
                                                           "\\u0192\\u0131\\u013C\\u00E6_\\u0148\\u00E5\\u0271" +
                                                           "\\u00EB";
    public static final String EMPTY_STRING = "";
    public static final String INVALID_MIME_TYPE = "meme/type";
    public static final String NULL_STRING = "null";
    private final ObjectMapper objectMapper = dtoObjectMapper;
    private CreateUploadHandler createUploadHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;
    private CustomerApiClient customerApiClient;
    private ResourceService resourceService;

    protected String getGeneratedKey(GatewayResponse<CreateUploadResponseBody> actual) throws JsonProcessingException {
        return actual.getBodyObject(CreateUploadResponseBody.class).key();
    }

    protected CreateUploadRequestBody createUploadRequestBody() {
        return new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected CreateUploadRequestBody createUploadRequestBodyNoFilename() {
        return new CreateUploadRequestBody(null, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected InitiateMultipartUploadResult uploadResult() {
        var uploadResult = new InitiateMultipartUploadResult();
        uploadResult.setKey(SAMPLE_UPLOAD_KEY);
        uploadResult.setUploadId(SAMPLE_UPLOAD_ID);
        return uploadResult;
    }

    @BeforeEach
    void setUp() {
        s3client = mock(AmazonS3Client.class);
        customerApiClient = mock(CustomerApiClient.class);
        resourceService = mock(ResourceService.class);
        createUploadHandler = new CreateUploadHandler(s3client, customerApiClient, resourceService);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void canCreateUpload() throws Exception {
        var resource = Resource.fromPublication(randomPublication());

        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(uploadResult());
        when(resourceService.getResourceByIdentifier(any())).thenReturn(resource);
        when(customerApiClient.fetch(any())).thenReturn(new Customer(Set.of(resource.getEntityDescription().getReference().getPublicationInstance().getInstanceType()), null, null));

        createUploadHandler.handleRequest(createUploadRequestWithBody(resource.getIdentifier(),
                                                                      createUploadRequestBody()),
                                          outputStream,
                                          context);

        var actual = GatewayResponse.fromOutputStream(outputStream, CreateUploadResponseBody.class);
        var actualBody = actual.getBodyObject(CreateUploadResponseBody.class);
        var expectedBody = new CreateUploadResponseBody(SAMPLE_UPLOAD_ID, getGeneratedKey(actual));

        assertThat(actualBody, is(equalTo(expectedBody)));
        assertThat(actual.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    void createUploadWithInvalidInputReturnBadRequest() throws Exception {
        createUploadHandler.handleRequest(createUploadRequestWithoutBody(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void createUploadWithS3ErrorReturnsNotFound() throws IOException, NotFoundException {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
            .thenThrow(SdkClientException.class);
        var resource = Resource.fromPublication(randomPublication());
        when(resourceService.getResourceByIdentifier(any())).thenReturn(resource);
        createUploadHandler.handleRequest(createUploadRequestWithBody(resource.getIdentifier(), createUploadRequestBody()), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void createUploadWithRuntimeErrorReturnsServerError() throws IOException, NotFoundException {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
            .thenThrow(RuntimeException.class);
        var resource = Resource.fromPublication(randomPublication());
        when(resourceService.getResourceByIdentifier(any())).thenReturn(resource);
        createUploadHandler.handleRequest(createUploadRequestWithBody(SortableIdentifier.next(), createUploadRequestBody()), outputStream,
                                          context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void setCreateUploadHandlerWithMissingFileParametersReturnsBadRequest() throws IOException {
        createUploadHandler.handleRequest(createUploadRequestWithBody(SortableIdentifier.next(),
                                                                      createUploadRequestBodyNoFilename()),
                                          outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream,
                                                                                              CreateUploadResponseBody.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void createUploadRequestBodyReturnsValidContentDispositionWhenInputIsValid() {
        var requestBody = new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING,
                                                                          SAMPLE_MIMETYPE);
        var actual = requestBody.toInitiateMultipartUploadRequest(randomString()).getObjectMetadata().getContentDisposition();
        var expected = generateContentDisposition(SAMPLE_FILENAME);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void createUploadRequestBodyReturnsValidContentDispositionWhenFilenameIsNull() {
        var requestBody = new CreateUploadRequestBody(null, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        var actual = requestBody.toInitiateMultipartUploadRequest(randomString()).getObjectMetadata().getContentDisposition();
        var expected = generateContentDisposition(NULL_STRING);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void createUploadRequestBodyReturnsValidContentDispositionWhenFilenameIsEmptyString() {
        var requestBody = new CreateUploadRequestBody(EMPTY_STRING, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        var actual = requestBody.toInitiateMultipartUploadRequest(randomString()).getObjectMetadata().getContentDisposition();
        var expected = generateContentDisposition(EMPTY_STRING);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void createUploadRequestBodyReturnsNullContentTypeWhenMimeTypeIsNull() {
        var requestBody = new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, null);
        var actual = requestBody.toInitiateMultipartUploadRequest(randomString()).getObjectMetadata().getContentType();

        assertThat(actual, is(nullValue()));
    }

    @Test
    void createUploadRequestBodyReturnsEmptyStringContentTypeWhenMimeTypeIsEmptyString() {
        var requestBody = new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, EMPTY_STRING);
        var actual = requestBody.toInitiateMultipartUploadRequest(randomString()).getObjectMetadata().getContentType();

        assertThat(actual, is(equalTo(EMPTY_STRING)));
    }

    @Test
    void createUploadRequestBodyReturnsContentTypeWhenMimeTypeIsInvalidString() {
        var requestBody = new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, INVALID_MIME_TYPE);
        var actual = requestBody.toInitiateMultipartUploadRequest(randomString()).getObjectMetadata().getContentType();

        assertThat(actual, is(equalTo(INVALID_MIME_TYPE)));
    }

    @Test
    void createUploadRequestReturnsValidContentDispositionWithEscapedUnicodeWhenInputIsUnicode() {
        var requestBody = new CreateUploadRequestBody(SAMPLE_UNICODE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        var actual = requestBody.toInitiateMultipartUploadRequest(randomString()).getObjectMetadata().getContentDisposition();
        var expected = generateContentDisposition(EXPECTED_ESCAPED_FILENAME);

        assertThat(actual, is(equalTo(expected)));
    }

    private String generateContentDisposition(String filename) {
        return String.format("filename=\"%s\"", filename);
    }

    private InputStream createUploadRequestWithBody(SortableIdentifier identifier, CreateUploadRequestBody uploadRequestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateUploadRequestBody>(objectMapper)
                   .withPathParameters(Map.of("publicationIdentifier", identifier.toString()))
                   .withBody(uploadRequestBody)
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .withClientId(randomString())
                   .build();
    }

    private InputStream createUploadRequestWithoutBody() throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateUploadRequestBody>(objectMapper)
                   .withBody(new CreateUploadRequestBody(null, null, null))
                   .build();
    }
}
