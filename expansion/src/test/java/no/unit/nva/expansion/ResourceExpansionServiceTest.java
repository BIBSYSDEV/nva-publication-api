package no.unit.nva.expansion;

import static no.unit.nva.expansion.ResourceExpansionServiceImpl.UNSUPPORTED_TYPE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceConversation;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceExpansionServiceTest extends ResourcesLocalTest {

    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final URI RESOURCE_OWNER_UNIT_AFFILIATION = URI.create("https://api.cristin.no/v2/units/194.63.10.0");
    public static final URI RESOURCE_OWNER_INSTITUTION_AFFILIATION =
        URI.create("https://api.cristin.no/v2/institutions/194");
    public static final Clock CLOCK = Clock.systemDefaultZone();
    private static final UserInstance SAMPLE_SENDER = sampleSender();
    private static final String PERSON_API_RESPONSE_WITH_UNIT =
        stringFromResources(Path.of("fake_person_api_response_with_unit.json"));
    private static final String PERSON_API_RESPONSE_WITH_INSTITUTION =
        stringFromResources(Path.of("fake_person_api_response_with_institution.json"));
    private static final String INSTITUTION_PROXY_ORG_RESPONSE =
        stringFromResources(Path.of("cristin_org.json"));
    private static final String INSTITUTION_PROXY_PARENT_ORG_RESPONSE =
        stringFromResources(Path.of("cristin_parent_org.json"));
    private static final String INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE =
        stringFromResources(Path.of("cristin_grand_parent_org.json"));
    private static final URI CRISTIN_ORG_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.63.10.0");
    private static final URI CRISTIN_ORG_PARENT_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.63.0.0");
    private static final URI CRISTIN_ORG_GRAND_PARENT_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.0.0.0");
    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private DoiRequestService doiRequestService;
    private FakeHttpClient<String> externalServicesHttpClient;

    @BeforeEach
    void setUp() throws Exception {
        super.init();
        externalServicesHttpClient = new FakeHttpClient<>(PERSON_API_RESPONSE_WITH_UNIT,
                                                          INSTITUTION_PROXY_ORG_RESPONSE,
                                                          INSTITUTION_PROXY_PARENT_ORG_RESPONSE,
                                                          INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE
        );
        initializeServices();
    }

    @Test
    void shouldReturnExpandedResourceConversationWithInstitutionsUriWhenPersonIsAffiliatedOnlyToInstitution()
        throws Exception {
        externalServicesHttpClient = new FakeHttpClient<>(PERSON_API_RESPONSE_WITH_INSTITUTION,
                                                          INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE
        );
        initializeServices();
        Publication createdPublication = createPublication(RESOURCE_OWNER_INSTITUTION_AFFILIATION);

        Message message = sendSupportMessage(createdPublication);

        ExpandedResourceConversation expandedResourceConversation =
            (ExpandedResourceConversation) expansionService.expandEntry(message);
        assertThat(expandedResourceConversation.getOrganizationIds(), containsInAnyOrder(CRISTIN_ORG_GRAND_PARENT_ID));
        assertThatHttpClientWasCalledOnceByAffiliationServiceAndOnceByExpansionService();
    }

    @Test
    void shouldReturnExpandedDoiRequestWithAllRelatedAffiliationsWhenResourceOwnerIsAffiliatedWithAUnit()
        throws Exception {
        Publication createdPublication = createPublication(RESOURCE_OWNER_UNIT_AFFILIATION);

        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(createdPublication));
        ExpandedDoiRequest expandedDoiRequest = (ExpandedDoiRequest) expansionService.expandEntry(doiRequest);
        assertThat(expandedDoiRequest.getOrganizationIds(), containsInAnyOrder(
            CRISTIN_ORG_ID,
            CRISTIN_ORG_PARENT_ID,
            CRISTIN_ORG_GRAND_PARENT_ID
        ));
    }

    @Test
    void shouldReturnExpandedResourceConversationWithAllRelatedAffiliationWhenOwnersAffiliationIsUnit()
        throws Exception {
        Publication createdPublication = createPublication(RESOURCE_OWNER_UNIT_AFFILIATION);
        Message message = sendSupportMessage(createdPublication);

        ExpandedResourceConversation expandedResourceConversation =
            (ExpandedResourceConversation) expansionService.expandEntry(message);
        assertThat(expandedResourceConversation.getOrganizationIds(), containsInAnyOrder(
            CRISTIN_ORG_ID,
            CRISTIN_ORG_PARENT_ID,
            CRISTIN_ORG_GRAND_PARENT_ID
        ));
    }

    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType)
        throws JsonProcessingException, NotFoundException {
        Publication publication = PublicationGenerator.randomPublication(instanceType);
        Resource resourceUpdate = Resource.fromPublication(publication);
        ExpandedResource indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        assertThat(indexDoc.fetchId(), is(not(nullValue())));
    }

    @ParameterizedTest(name = "should process all ResourceUpdate types:{0}")
    @MethodSource("listResourceUpdateTypes")
    void shouldProcessAllResourceUpdateTypes(Class<?> resourceUpdateType)
        throws IOException, ApiGatewayException {
        DataEntry resource = generateResourceUpdate(resourceUpdateType);
        expansionService.expandEntry(resource);
        assertDoesNotThrow(() -> expansionService.expandEntry(resource));
    }

    @Test
    void shouldIncludeAllDoiRequestMessagesInExpandedDoiRequest() throws ApiGatewayException, JsonProcessingException {
        var samplePublication = PublicationGenerator.randomPublication();
        var userInstance = extractUserInstance(samplePublication);
        samplePublication = resourceService.createPublication(userInstance, samplePublication);
        var doiRequestIdentifier = doiRequestService.createDoiRequest(samplePublication);
        var expectedDoiRequestMessageIdentifiers = sendSomeDoiRequestMessages(samplePublication);
        var expandedDoiRequest = expandDoiRequest(userInstance, doiRequestIdentifier);

        var messagesIdentifiersInExpandedDoiRequest = expandedDoiRequest.getDoiRequestMessages().getMessages()
            .stream()
            .map(MessageDto::getMessageIdentifier)
            .collect(Collectors.toList());
        assertThat(messagesIdentifiersInExpandedDoiRequest,
                   contains(expectedDoiRequestMessageIdentifiers.toArray(SortableIdentifier[]::new)));
    }

    private static List<Class<?>> listResourceUpdateTypes() {
        JsonSubTypes[] annotations = DataEntry.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value).collect(Collectors.toList());
    }

    private static List<Class<?>> listPublicationInstanceTypes() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes();
    }

    private static UserInstance sampleSender() {
        return new UserInstance(SOME_SENDER, SOME_ORG);
    }

    private Message sendSupportMessage(Publication createdPublication)
        throws TransactionFailedException, NotFoundException {
        UserInstance userInstance = UserInstance.fromPublication(createdPublication);
        SortableIdentifier identifier = messageService.createSimpleMessage(userInstance,
                                                                           createdPublication,
                                                                           randomString());
        return messageService.getMessage(userInstance, identifier);
    }

    private List<SortableIdentifier> sendSomeDoiRequestMessages(Publication samplePublication)
        throws TransactionFailedException {
        return List.of(
            sendSomeDoiRequestMessage(samplePublication),
            sendSomeDoiRequestMessage(samplePublication)
        );
    }

    private ExpandedDoiRequest expandDoiRequest(UserInstance userInstance, SortableIdentifier doiRequestIdentifier)
        throws NotFoundException, JsonProcessingException {
        var doiRequestDataEntry = doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);
        return (ExpandedDoiRequest) expansionService.expandEntry(doiRequestDataEntry);
    }

    private SortableIdentifier sendSomeDoiRequestMessage(Publication samplePublication)
        throws TransactionFailedException {
        var userInstance = extractUserInstance(samplePublication);
        return messageService.createDoiRequestMessage(userInstance, samplePublication, randomString());
    }

    private void initializeServices() {
        resourceService = new ResourceService(client, externalServicesHttpClient, CLOCK);
        messageService = new MessageService(client, CLOCK);
        doiRequestService = new DoiRequestService(client, externalServicesHttpClient, CLOCK);
        expansionService = new ResourceExpansionServiceImpl(externalServicesHttpClient, resourceService,
                                                            messageService);
    }

    private void assertThatHttpClientWasCalledOnceByAffiliationServiceAndOnceByExpansionService() {
        assertThat(externalServicesHttpClient.getCallCounter().get(), is(equalTo(2)));
    }

    private Publication createPublication(URI resourceOwnerAffiliation) throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        UserInstance userInstance = extractUserInstance(publication);
        var createdPublication = resourceService.createPublication(userInstance, publication);
        assertThat(createdPublication.getResourceOwner().getOwnerAffiliation(), is(equalTo(
            resourceOwnerAffiliation)));
        return createdPublication;
    }

    private DataEntry generateResourceUpdate(Class<?> resourceUpdateType) throws ApiGatewayException {
        Publication createdPublication = createPublication(RESOURCE_OWNER_UNIT_AFFILIATION);

        if (Resource.class.equals(resourceUpdateType)) {
            return Resource.fromPublication(createdPublication);
        }
        if (DoiRequest.class.equals(resourceUpdateType)) {
            return createDoiRequest(createdPublication);
        }
        if (Message.class.equals(resourceUpdateType)) {
            return sendSupportMessage(createdPublication);
        }
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + resourceUpdateType.getSimpleName());
    }

    private DoiRequest createDoiRequest(Publication createdPublication) {
        Resource resource = Resource.fromPublication(createdPublication);

        return DoiRequest.newDoiRequestForResource(resource);
    }
}
