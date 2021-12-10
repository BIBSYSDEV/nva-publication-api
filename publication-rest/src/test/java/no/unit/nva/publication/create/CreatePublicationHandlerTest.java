package no.unit.nva.publication.create;

import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.create.CreatePublicationHandler.API_HOST;
import static no.unit.nva.publication.create.CreatePublicationHandler.API_SCHEME;
import static no.unit.nva.publication.testing.TestHeaders.getResponseHeaders;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreatePublicationHandlerTest extends ResourcesLocalTest {

    public static final String HTTPS = "https";
    public static final String NVA_UNIT_NO = "nva.unit.no";
    public static final String WILDCARD = "*";
    public static final String TEST_FEIDE_ID = randomString();
    public static final URI TEST_ORG_ID = randomUri();

    public static final Clock CLOCK = Clock.systemDefaultZone();
    private CreatePublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        super.init();
        Environment environmentMock = mock(Environment.class);
        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environmentMock.readEnv(API_SCHEME)).thenReturn(HTTPS);
        when(environmentMock.readEnv(API_HOST)).thenReturn(NVA_UNIT_NO);
        ResourceService resourceService = new ResourceService(client, new FakeHttpClient(), CLOCK);
        handler = new CreatePublicationHandler(resourceService, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
    }

    @Test
    public void requestToHandlerReturnsMinRequiredFieldsWhenRequestContainsEmptyResource() throws Exception {

        CreatePublicationRequest request = createEmptyPublicationRequest();
        InputStream inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<PublicationResponse> actual = GatewayResponse.fromOutputStream(outputStream);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }

    private CreatePublicationRequest createEmptyPublicationRequest() {
        return new CreatePublicationRequest();
    }

    @Test
    public void requestToHandlerReturnsResourceWithFilSetWhenRequestContainsFileSet() throws Exception {
        FileSet filesetInCreationRequest = PublicationGenerator.randomPublication().getFileSet();
        CreatePublicationRequest request = createEmptyPublicationRequest();
        request.setFileSet(filesetInCreationRequest);

        InputStream inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<PublicationResponse> actual = GatewayResponse.fromOutputStream(outputStream);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertThat(publicationResponse.getFileSet(),is(equalTo(filesetInCreationRequest)));
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }

    private void assertExistenceOfMinimumRequiredFields(PublicationResponse publicationResponse) {
        assertThat(publicationResponse.getIdentifier(), is(not(nullValue())));
        assertThat(publicationResponse.getIdentifier(), is(instanceOf(SortableIdentifier.class)));
        assertThat(publicationResponse.getCreatedDate(), is(not(nullValue())));
        assertThat(publicationResponse.getOwner(), is(equalTo(TEST_FEIDE_ID)));
        assertThat(publicationResponse.getPublisher().getId(), is(equalTo(TEST_ORG_ID)));
    }

    private Map<String, String> getResponseHeadersWithLocation(SortableIdentifier identifier) {
        Map<String, String> map = new HashMap<>(getResponseHeaders());
        map.put(HttpHeaders.LOCATION, handler.getLocation(identifier).toString());
        return map;
    }

    private InputStream createPublicationRequest(CreatePublicationRequest request) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
            .withFeideId(TEST_FEIDE_ID)
            .withCustomerId(TEST_ORG_ID.toString())
            .withBody(request)
            .build();
    }
}
