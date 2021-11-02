package no.unit.nva.expansion;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.ResourceExpansionServiceImpl.UNSUPPORTED_TYPE;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.expansion.model.CustomerResponse;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.InstitutionResponse;
import no.unit.nva.expansion.model.UserResponse;
import no.unit.nva.expansion.restclients.IdentityClient;
import no.unit.nva.expansion.restclients.IdentityClientImpl;
import no.unit.nva.expansion.restclients.InstitutionClient;
import no.unit.nva.expansion.restclients.InstitutionClientImpl;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.PublicationInstanceBuilder;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceExpansionServiceTest {

    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final String SOME_MESSAGE = "someMessage";
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
    public static final int ORGANIZATION_AND_TWO_SUBUNITS = 3;
    public static final int NO_ORGANIZATION = 0;
    private static final UserInstance SAMPLE_SENDER = sampleSender();
    private ResourceExpansionService service;
    private HttpClient httpClientMock;

    @BeforeEach
    void init() throws Exception {
        SecretsReader secretsReaderMock = createSecretsReaderMockAlwaysReturnsSecret();
        httpClientMock = mock(HttpClient.class);
        IdentityClient identityClient = new IdentityClientImpl(secretsReaderMock, httpClientMock);
        InstitutionClient institutionClient = new InstitutionClientImpl(httpClientMock);
        service = new ResourceExpansionServiceImpl(identityClient, institutionClient);
    }

    @Test
    void shouldReturnExpandedMessageWithTheOrgIdsThatTheRespectiveResourceOwnerIsAffiliatedWith() throws Exception {
        prepareHttpClientMockReturnsUserThenCustomerThenInstitutionWithTwoSubunits();
        Message message = createMessage();
        ExpandedMessage expandedMessage = (ExpandedMessage) service.expandEntry(message);

        assertThat(expandedMessage.getOrganizationIds().size(), is(ORGANIZATION_AND_TWO_SUBUNITS));
        assertThatExpandedMessageHasNoDataLoss(message, expandedMessage);
    }

    @Test
    void shouldReturnExpandedDoiRequestWithTheOrgIdsThatTheRespectiveResourceOwnerIsAffiliatedWith() throws Exception {
        prepareHttpClientMockReturnsUserThenCustomerThenInstitutionWithTwoSubunits();

        DoiRequest doiRequest = createDoiRequest();
        ExpandedDoiRequest expandedDoiRequest = (ExpandedDoiRequest) service.expandEntry(doiRequest);

        assertThat(expandedDoiRequest.getOrganizationIds().size(), is(ORGANIZATION_AND_TWO_SUBUNITS));
        assertThatExpandedDoiRequestHasNoDataLoss(doiRequest, expandedDoiRequest);
    }

    @Test
    void shouldReturnExpandedMessageWithEmptyOrganizationIdsOnNoResourceOwnerUserResponse() throws Exception {
        prepareHttpClientMockReturnsNothing();

        Message message = createMessage();
        ExpandedMessage expandedMessage = (ExpandedMessage) service.expandEntry(message);

        assertThat(expandedMessage.getOrganizationIds().size(), is(NO_ORGANIZATION));
        assertThatExpandedMessageHasNoDataLoss(message, expandedMessage);
    }

    @Test
    void shouldReturnExpandedDoiRequestWithEmptyOrganizationIdsOnNoResourceOwnerCustomerResponse() throws Exception {
        prepareHttpClientMockReturnsUser();

        DoiRequest doiRequest = createDoiRequest();
        ExpandedDoiRequest expandedDoiRequest = (ExpandedDoiRequest) service.expandEntry(doiRequest);

        assertThat(expandedDoiRequest.getOrganizationIds().size(), is(NO_ORGANIZATION));
        assertThatExpandedDoiRequestHasNoDataLoss(doiRequest, expandedDoiRequest);
    }

    @Test
    void shouldReturnExpandedDoiRequestWithEmptyOrganizationIdsOnNoResourceOwnerInstitutionResponse() throws Exception {
        prepareHttpClientMockReturnsUserThenCustomer();

        DoiRequest doiRequest = createDoiRequest();
        ExpandedDoiRequest expandedDoiRequest = (ExpandedDoiRequest) service.expandEntry(doiRequest);

        assertThat(expandedDoiRequest.getOrganizationIds().size(), is(NO_ORGANIZATION));
        assertThatExpandedDoiRequestHasNoDataLoss(doiRequest, expandedDoiRequest);
    }

    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType) throws JsonProcessingException {
        Publication publication = PublicationGenerator.randomPublication(instanceType);
        Resource resourceUpdate = Resource.fromPublication(publication);
        ExpandedResource indexDoc = (ExpandedResource) service.expandEntry(resourceUpdate);
        assertThat(indexDoc.getId(), is(not(nullValue())));
    }

    @ParameterizedTest(name = "should process all ResourceUpdate types:{0}")
    @MethodSource("listResourceUpdateTypes")
    void shouldProcessAllResourceUpdateTypes(Class<?> resourceUpdateType) throws IOException, InterruptedException {
        prepareHttpClientMockReturnsUserThenCustomerThenInstitutionWithTwoSubunits();
        ResourceUpdate resource = generateResourceUpdate(resourceUpdateType);
        service.expandEntry(resource);
        assertDoesNotThrow(() -> service.expandEntry(resource));
    }

    private ResourceUpdate generateResourceUpdate(Class<?> resourceUpdateType) {
        if (Resource.class.equals(resourceUpdateType)) {
            return Resource.fromPublication(PublicationGenerator.randomPublication());
        }
        if (DoiRequest.class.equals(resourceUpdateType)) {
            return createDoiRequest();
        }
        if (Message.class.equals(resourceUpdateType)) {
            return createMessage();
        }
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + resourceUpdateType.getSimpleName());
    }

    private static List<Class<?>> listResourceUpdateTypes() {
        JsonSubTypes[] annotations = ResourceUpdate.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value).collect(Collectors.toList());
    }

    private static List<Class<?>> listPublicationInstanceTypes() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes();
    }

    private static UserInstance sampleSender() {
        return new UserInstance(SOME_SENDER, SOME_ORG);
    }

    private void assertThatExpandedMessageHasNoDataLoss(Message message, Message expandedMessage) {
        assertThat(expandedMessage, is(equalTo(message)));
    }

    private void assertThatExpandedDoiRequestHasNoDataLoss(DoiRequest doiRequest, DoiRequest expandedDoiRequest) {
        assertThat(expandedDoiRequest, is(equalTo(doiRequest)));
    }

    private DoiRequest createDoiRequest() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        Resource resource = Resource.fromPublication(publication);

        return DoiRequest.newDoiRequestForResource(resource);
    }

    private Message createMessage() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        SortableIdentifier messageIdentifier = SortableIdentifier.next();

        Message message = Message.supportMessage(SAMPLE_SENDER, publication, SOME_MESSAGE, messageIdentifier, CLOCK);
        return message;
    }

    private HttpClient prepareHttpClientMockReturnsNothing() throws IOException, InterruptedException {
        when(httpClientMock.send(any(), any()))
            .thenThrow(IOException.class);

        return httpClientMock;
    }

    private HttpClient prepareHttpClientMockReturnsUser() throws IOException, InterruptedException {
        HttpResponse<String> userResponse = createHttpResponse(createUserResponseAsJson());
        when(httpClientMock.<String>send(any(), any()))
            .thenReturn(userResponse)
            .thenThrow(IOException.class);

        return httpClientMock;
    }

    private HttpClient prepareHttpClientMockReturnsUserThenCustomer() throws IOException, InterruptedException {
        HttpResponse<String> userResponse = createHttpResponse(createUserResponseAsJson());
        HttpResponse<String> customerResponse = createHttpResponse(createCustomerResponseAsJson());
        when(httpClientMock.<String>send(any(), any()))
            .thenReturn(userResponse)
            .thenReturn(customerResponse)
            .thenThrow(IOException.class);

        return httpClientMock;
    }

    private HttpClient prepareHttpClientMockReturnsUserThenCustomerThenInstitutionWithTwoSubunits()
        throws IOException, InterruptedException {
        HttpResponse<String> userResponse = createHttpResponse(createUserResponseAsJson());
        HttpResponse<String> customerResponse = createHttpResponse(createCustomerResponseAsJson());
        HttpResponse<String> institutionResponse = createHttpResponse(createInstitutionResponseAsJson());
        when(httpClientMock.<String>send(any(), any()))
            .thenReturn(userResponse)
            .thenReturn(customerResponse)
            .thenReturn(institutionResponse);

        return httpClientMock;
    }

    private HttpResponse<String> createHttpResponse(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);

        return response;
    }

    private SecretsReader createSecretsReaderMockAlwaysReturnsSecret() throws ErrorReadingSecretException {
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(any(), any())).thenReturn("secret");

        return secretsReader;
    }

    private String createCustomerResponseAsJson() throws JsonProcessingException {
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setCristinId(randomUri());

        return objectMapper.writeValueAsString(customerResponse);
    }

    private String createUserResponseAsJson() throws JsonProcessingException {
        UserResponse userResponse = new UserResponse();
        userResponse.setCustomerId(randomUri());

        return objectMapper.writeValueAsString(userResponse);
    }

    private String createInstitutionResponseAsJson() throws JsonProcessingException {
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId(randomUri());
        InstitutionResponse.SubUnit subUnit1 = new InstitutionResponse.SubUnit();
        subUnit1.setId(randomUri());
        InstitutionResponse.SubUnit subUnit2 = new InstitutionResponse.SubUnit();
        subUnit2.setId(randomUri());
        institutionResponse.setSubunits(List.of(subUnit1, subUnit2));

        return objectMapper.writeValueAsString(institutionResponse);
    }
}
