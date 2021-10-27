package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceExpansionServiceTest {

    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final UserInstance SAMPLE_OWNER = new UserInstance("sample@owner", SOME_ORG);
    public static final String SOME_MESSAGE = "someMessage";
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
    private static final UserInstance SAMPLE_SENDER = sampleSender();

    private final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private ResourceExpansionService service;

    @Test
    void shouldReturnExpandedMessageWithOrganizationIds() throws IOException, ErrorReadingSecretException, InterruptedException {
        SecretsReader secretsReader = createSecretsReaderMockAlwaysReturnsSecret();
        Environment environment = createEnvironmentMock();
        HttpClient httpClient = createHttpClientMockReturnsUserThenCustomer();

        IdentityClient identityClient = new IdentityClientImpl(secretsReader, environment, httpClient);

        service = new ResourceExpansionServiceImpl(identityClient);

        Message message = createMessage();

        ExpandedMessage expandedMessage = service.expandMessage(message);

        assertThat(expandedMessage.getOrganizationIds().size(), is(1));
    }

    @Test
    void shouldReturnExpandedDoiRequestWithOrganizationIds() throws IOException, ErrorReadingSecretException, InterruptedException {
        SecretsReader secretsReader = createSecretsReaderMockAlwaysReturnsSecret();
        Environment environment = createEnvironmentMock();
        HttpClient httpClient = createHttpClientMockReturnsUserThenCustomer();

        IdentityClient identityClient = new IdentityClientImpl(secretsReader, environment, httpClient);

        service = new ResourceExpansionServiceImpl(identityClient);

        DoiRequest doiRequest = createDoiRequest();

        ExpandedDoiRequest expandedDoiRequest = service.expandDoiRequest(doiRequest);

        assertThat(expandedDoiRequest.getOrganizationIds().size(), is(1));
    }

    private DoiRequest createDoiRequest() {
        SortableIdentifier resourceIdentifier = SortableIdentifier.next();
        Publication publication = samplePublication(resourceIdentifier);
        Resource resource = Resource.fromPublication(publication);

        return DoiRequest.newDoiRequestForResource(resource);
    }

    private Message createMessage() {
        SortableIdentifier resourceIdentifier = SortableIdentifier.next();
        Publication publication = samplePublication(resourceIdentifier);
        SortableIdentifier messageIdentifier = SortableIdentifier.next();

        Message message = Message.supportMessage(SAMPLE_SENDER, publication, SOME_MESSAGE, messageIdentifier, CLOCK);
        return message;
    }

    private HttpClient createHttpClientMockReturnsUserThenCustomer() throws IOException, InterruptedException {
        HttpResponse userResponse = mock(HttpResponse.class);
        when(userResponse.body()).thenReturn(createUserResponseAsJson());

        HttpResponse customerResponse = mock(HttpResponse.class);
        when(customerResponse.body()).thenReturn(createCustomerResponseAsJson());

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any()))
                .thenReturn(userResponse)
                .thenReturn(customerResponse);

        return httpClient;
    }

    private Environment createEnvironmentMock() {
        Environment environment = mock(Environment.class);
        return environment;
    }

    private SecretsReader createSecretsReaderMockAlwaysReturnsSecret() throws ErrorReadingSecretException {
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(any(), any())).thenReturn("secret");
        return secretsReader;
    }

    private Object createCustomerResponseAsJson() throws JsonProcessingException {
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setCristinId(randomUri());
        return objectMapper.writeValueAsString(customerResponse);
    }

    private String createUserResponseAsJson() throws JsonProcessingException {
        UserResponse userResponse = new UserResponse();
        userResponse.setCustomerId(randomUri());
        return objectMapper.writeValueAsString(userResponse);
    }

    private static UserInstance sampleSender() {
        return new UserInstance(SOME_SENDER, SOME_ORG);
    }

    private Publication samplePublication(SortableIdentifier resourceIdentifier) {
        Organization publisher = new Organization.Builder().withId(SAMPLE_OWNER.getOrganizationUri()).build();
        EntityDescription entityDescription = new EntityDescription.Builder().withMainTitle(randomString()).build();
        return new Publication.Builder()
                .withPublisher(publisher)
                .withOwner(SAMPLE_OWNER.getUserIdentifier())
                .withIdentifier(resourceIdentifier)
                .withEntityDescription(entityDescription)
                .build();
    }

}
