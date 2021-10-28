package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.impl.IdentityClientImpl;
import no.unit.nva.expansion.impl.InstitutionClientImpl;
import no.unit.nva.expansion.impl.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.CustomerResponse;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.InstitutionResponse;
import no.unit.nva.expansion.model.UserResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static no.unit.nva.expansion.JsonConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceExpansionServiceTest {

    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final String SOME_MESSAGE = "someMessage";
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
    private static final UserInstance SAMPLE_SENDER = sampleSender();
    public static final int ORGANIZATION_AND_TWO_SUBUNITS = 3;
    public static final int NO_ORGANIZATION = 0;

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
        ExpandedMessage expandedMessage = service.expandMessage(message);

        assertThat(expandedMessage.getOrganizationIds().size(), is(ORGANIZATION_AND_TWO_SUBUNITS));
        assertThat((Message) expandedMessage, is(equalTo(message)));
    }

    @Test
    void shouldReturnExpandedDoiRequestWithTheOrgIdsThatTheRespectiveResourceOwnerIsAffiliatedWith() throws Exception {
        prepareHttpClientMockReturnsUserThenCustomerThenInstitutionWithTwoSubunits();

        DoiRequest doiRequest = createDoiRequest();
        ExpandedDoiRequest expandedDoiRequest = service.expandDoiRequest(doiRequest);

        assertThat(expandedDoiRequest.getOrganizationIds().size(), is(ORGANIZATION_AND_TWO_SUBUNITS));
        assertThat((DoiRequest) expandedDoiRequest, is(equalTo(doiRequest)));
    }

    @Test
    void shouldReturnExpandedMessageWithEmptyOrganizationIdsOnNoResourceOwnerUserResponse() throws Exception {
        prepareHttpClientMockReturnsNothing();

        Message message = createMessage();
        ExpandedMessage expandedMessage = service.expandMessage(message);

        assertThat(expandedMessage.getOrganizationIds().size(), is(NO_ORGANIZATION));
        assertThat((Message) expandedMessage, is(equalTo(message)));
    }

    @Test
    void shouldReturnExpandedDoiRequestWithEmptyOrganizationIdsOnNoResourceOwnerCustomerResponse() throws Exception {
        prepareHttpClientMockReturnsUser();

        DoiRequest doiRequest = createDoiRequest();
        ExpandedDoiRequest expandedDoiRequest = service.expandDoiRequest(doiRequest);

        assertThat(expandedDoiRequest.getOrganizationIds().size(), is(NO_ORGANIZATION));
        assertThat((DoiRequest) expandedDoiRequest, is(equalTo(doiRequest)));
    }

    @Test
    void shouldReturnExpandedDoiRequestWithEmptyOrganizationIdsOnNoResourceOwnerInstitutionResponse() throws Exception {
        prepareHttpClientMockReturnsUserThenCustomer();

        DoiRequest doiRequest = createDoiRequest();
        ExpandedDoiRequest expandedDoiRequest = service.expandDoiRequest(doiRequest);

        assertThat(expandedDoiRequest.getOrganizationIds().size(), is(NO_ORGANIZATION));
        assertThat((DoiRequest) expandedDoiRequest, is(equalTo(doiRequest)));
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
        HttpResponse userResponse = createHttpResponse(createUserResponseAsJson());
        when(httpClientMock.send(any(), any()))
                .thenReturn(userResponse)
                .thenThrow(IOException.class);

        return httpClientMock;
    }

    private HttpClient prepareHttpClientMockReturnsUserThenCustomer() throws IOException, InterruptedException {
        HttpResponse userResponse = createHttpResponse(createUserResponseAsJson());
        HttpResponse customerResponse = createHttpResponse(createCustomerResponseAsJson());
        when(httpClientMock.send(any(), any()))
                .thenReturn(userResponse)
                .thenReturn(customerResponse)
                .thenThrow(IOException.class);

        return httpClientMock;
    }

    private HttpClient prepareHttpClientMockReturnsUserThenCustomerThenInstitutionWithTwoSubunits()
            throws IOException, InterruptedException {
        HttpResponse userResponse = createHttpResponse(createUserResponseAsJson());
        HttpResponse customerResponse = createHttpResponse(createCustomerResponseAsJson());
        HttpResponse institutionResponse = createHttpResponse(createInstitutionResponseAsJson());
        when(httpClientMock.send(any(), any()))
                .thenReturn(userResponse)
                .thenReturn(customerResponse)
                .thenReturn(institutionResponse);

        return httpClientMock;
    }

    private HttpResponse createHttpResponse(String body) {
        HttpResponse userResponse = mock(HttpResponse.class);
        when(userResponse.statusCode()).thenReturn(200);
        when(userResponse.body()).thenReturn(body);

        return userResponse;
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

    private static UserInstance sampleSender() {
        return new UserInstance(SOME_SENDER, SOME_ORG);
    }
}
