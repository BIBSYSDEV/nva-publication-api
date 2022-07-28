package no.unit.nva.publication.create;

import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.create.CreatePublicationHandler.API_HOST;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.core.attempt.Try.attempt;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class CreatePublicationHandlerTest extends ResourcesLocalTest {
    
    public static final String NVA_UNIT_NO = "nva.unit.no";
    public static final String WILDCARD = "*";
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final Clock CLOCK = Clock.systemDefaultZone();
    private String testUserName;
    private URI testOrgId;
    private CreatePublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private Publication samplePublication;
    private URI topLevelCristinOrgId;
    
    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        super.init();
        Environment environmentMock = mock(Environment.class);
        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environmentMock.readEnv(API_HOST)).thenReturn(NVA_UNIT_NO);
        ResourceService resourceService = new ResourceService(client, CLOCK);
        handler = new CreatePublicationHandler(resourceService, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        samplePublication = PublicationGenerator.randomPublication();
        testUserName = samplePublication.getResourceOwner().getOwner();
        testOrgId = samplePublication.getPublisher().getId();
        topLevelCristinOrgId = randomUri();
    }
    
    @Test
    void requestToHandlerReturnsMinRequiredFieldsWhenRequestBodyIsEmpty()
        throws Exception {
        var inputStream = createPublicationRequest(null);
        handler.handleRequest(inputStream, outputStream, context);
        
        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }
    
    @Test
    void requestToHandlerReturnsMinRequiredFieldsWhenRequestContainsEmptyResource() throws Exception {
        var request = createEmptyPublicationRequest();
        InputStream inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);
        
        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }
    
    @Test
    void requestToHandlerReturnsResourceWithFilSetWhenRequestContainsFileSet() throws Exception {
        var filesetInCreationRequest = PublicationGenerator.randomPublication().getFileSet();
        var request = createEmptyPublicationRequest();
        request.setFileSet(filesetInCreationRequest);
        
        var inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);
        
        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertThat(publicationResponse.getFileSet(), is(equalTo(filesetInCreationRequest)));
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }
    
    @Test
    void shouldSaveAllSuppliedInformationOfPublicationRequestExceptForInternalInformationDecidedByService()
        throws Exception {
        var request = CreatePublicationRequest.fromPublication(samplePublication);
        var inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);
        
        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        
        var actualPublicationResponse = actual.getBodyObject(PublicationResponse.class);
        
        var expectedPublicationResponse =
            constructResponseSettingFieldsThatAreNotCopiedByTheRequest(samplePublication, actualPublicationResponse);
        
        var diff = JAVERS.compare(expectedPublicationResponse, actualPublicationResponse);
        assertThat(actualPublicationResponse.getIdentifier(), is(equalTo(expectedPublicationResponse.getIdentifier())));
        assertThat(actualPublicationResponse.getPublisher(), is(equalTo(expectedPublicationResponse.getPublisher())));
        assertThat(diff.prettyPrint(), actualPublicationResponse, is(equalTo(expectedPublicationResponse)));
    }
    
    @Test
    void shouldReturnUnauthorizedWhenUserCannotBeIdentified() throws IOException {
        var event = requestWithoutUsername(createEmptyPublicationRequest());
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }
    
    private CreatePublicationRequest createEmptyPublicationRequest() {
        return new CreatePublicationRequest();
    }
    
    private PublicationResponse constructResponseSettingFieldsThatAreNotCopiedByTheRequest(
        Publication samplePublication, PublicationResponse actualPublicationResponse) {
        var expectedPublication = setAllFieldsThatAreNotCopiedFromTheCreateRequest(samplePublication,
            actualPublicationResponse);
        return PublicationResponse.fromPublication(expectedPublication);
    }
    
    private Publication setAllFieldsThatAreNotCopiedFromTheCreateRequest(
        Publication samplePublication, PublicationResponse actualPublicationResponse) {
        return attempt(() -> removeAllFieldsThatAreNotCopiedFromTheCreateRequest(samplePublication))
            .map(publication ->
                     setAllFieldsThatAreAutomaticallySetByResourceService(publication, actualPublicationResponse))
            .orElseThrow();
    }
    
    private Publication setAllFieldsThatAreAutomaticallySetByResourceService(
        Publication samplePublication,
        PublicationResponse actualPublicationResponse) {
        return samplePublication.copy()
            .withIdentifier(actualPublicationResponse.getIdentifier())
            .withCreatedDate(actualPublicationResponse.getCreatedDate())
            .withModifiedDate(actualPublicationResponse.getModifiedDate())
            .withIndexedDate(actualPublicationResponse.getIndexedDate())
            .withStatus(PublicationStatus.DRAFT)
            .build();
    }
    
    private Publication removeAllFieldsThatAreNotCopiedFromTheCreateRequest(Publication samplePublication) {
        return samplePublication.copy()
            .withDoi(null)
            .withHandle(null)
            .withLink(null)
            .withPublishedDate(null)
            .withPublisher(new Organization.Builder().withLabels(null).withId(testOrgId).build())
            //            .withResourceOwner(null)
            .build();
    }
    
    private void assertExistenceOfMinimumRequiredFields(PublicationResponse publicationResponse) {
        assertThat(publicationResponse.getIdentifier(), is(not(nullValue())));
        assertThat(publicationResponse.getIdentifier(), is(instanceOf(SortableIdentifier.class)));
        assertThat(publicationResponse.getCreatedDate(), is(not(nullValue())));
        assertThat(publicationResponse.getResourceOwner().getOwner(), is(equalTo(testUserName)));
        assertThat(publicationResponse.getResourceOwner().getOwnerAffiliation(), is(equalTo(topLevelCristinOrgId)));
        assertThat(publicationResponse.getPublisher().getId(), is(equalTo(testOrgId)));
    }
    
    private InputStream createPublicationRequest(CreatePublicationRequest request) throws JsonProcessingException {
        
        return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
            .withNvaUsername(testUserName)
            .withCustomerId(testOrgId)
            .withTopLevelCristinOrgId(topLevelCristinOrgId)
            .withBody(request)
            .build();
    }
    
    private InputStream requestWithoutUsername(CreatePublicationRequest request) throws JsonProcessingException {
        
        return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
            .withCustomerId(testOrgId)
            .withBody(request)
            .build();
    }
}
