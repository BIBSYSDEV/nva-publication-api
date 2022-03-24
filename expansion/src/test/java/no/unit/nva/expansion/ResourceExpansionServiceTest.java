package no.unit.nva.expansion;

import static no.unit.nva.expansion.ResourceExpansionServiceImpl.UNSUPPORTED_TYPE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceConversation;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageCollection;
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

class ResourceExpansionServiceTest extends ResourcesLocalTest {

    public static final URI RESOURCE_OWNER_UNIT_AFFILIATION = URI.create("https://api.cristin.no/v2/units/194.63.10.0");
    public static final URI RESOURCE_OWNER_INSTITUTION_AFFILIATION =
        URI.create("https://api.cristin.no/v2/institutions/194");
    public static final Clock CLOCK = Clock.systemDefaultZone();

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
    void setUp() {
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
    void shouldIncludeAllDoiRequestMessagesInExpandedDoiRequestWhenDoiRequestIsCreated()
        throws ApiGatewayException, JsonProcessingException {
        var samplePublication = createSamplePublicationWithConversations();
        var expandedDoiRequestConversation =
            (ExpandedDoiRequest) expansionService.expandEntry(samplePublication.getDoiRequest());

        assertThatExpandedDoiRequestContainsOnlyDoiRequestMessagesAndNotAnyOfTheSupportMessages(
            samplePublication, expandedDoiRequestConversation);
    }

    @Test
    void shouldIncludeAllDoiRequestMessagesInExpandedDoiRequestWhenDoiRequestMessageIsSent()
        throws ApiGatewayException, JsonProcessingException {
        var samplePublication =
            createSamplePublicationWithConversations();
        var expandedDoiRequestConversation =
            (ExpandedDoiRequest) expansionService.expandEntry(samplePublication.getLastDoiRequestMessage());

        assertThatExpandedDoiRequestContainsOnlyDoiRequestMessagesAndNotAnyOfTheSupportMessages(
            samplePublication, expandedDoiRequestConversation);
    }

    @Test
    void shouldIncludeSupportMessagesAndExcludeAllDoiRequestMessagesFromGeneralSupportConversations()
        throws ApiGatewayException, JsonProcessingException {
        var samplePublication =
            createSamplePublicationWithConversations();

        var expandedGeneralSupportConversation =
            (ExpandedResourceConversation) expansionService.expandEntry(samplePublication.getLastSupportMessage());

        assertThatSupportConversationIncludesSupportMessagesAndExcludesDoiRequestMessages(
            samplePublication, expandedGeneralSupportConversation);
    }

    private static List<Class<?>> listResourceUpdateTypes() {
        JsonSubTypes[] annotations = DataEntry.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value).collect(Collectors.toList());
    }

    private static List<Class<?>> listPublicationInstanceTypes() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes();
    }

    private void assertThatExpandedDoiRequestContainsOnlyDoiRequestMessagesAndNotAnyOfTheSupportMessages(
        PublicationWithDoiRequestAndAllTypesOfMessages samplePublication,
        ExpandedDoiRequest expandedDoiRequestConversation) {
        var messagesIdentifiersInExpandedDoiRequest =
            extractMessageIdentifiersFromExpandedDoiRequest(expandedDoiRequestConversation);

        assertThat(messagesIdentifiersInExpandedDoiRequest,
                   contains(samplePublication.getDoiRequestMessageIdentifiers()));
        assertThatNoItemInNonDesiredCollectionExistsInTheActualCollection(
            samplePublication.getSupportMessageIdentifiers(), messagesIdentifiersInExpandedDoiRequest);
    }

    private void assertThatSupportConversationIncludesSupportMessagesAndExcludesDoiRequestMessages(
        PublicationWithDoiRequestAndAllTypesOfMessages samplePublication,
        ExpandedResourceConversation expandedGeneralSupportConversation) {
        var actualMessageIdentifiers = expandedGeneralSupportConversation.getMessageCollections()
            .stream()
            .map(MessageCollection::getMessages)
            .flatMap(Collection::stream)
            .map(MessageDto::getMessageIdentifier)
            .collect(Collectors.toList());

        assertThat(actualMessageIdentifiers, contains(samplePublication.getSupportMessageIdentifiers()));
        assertThatNoItemInNonDesiredCollectionExistsInTheActualCollection(
            samplePublication.getDoiRequestMessageIdentifiers(), actualMessageIdentifiers);
    }

    private <T> void assertThatNoItemInNonDesiredCollectionExistsInTheActualCollection(
        T[] nonDesiredArray, Collection<T> actualCollection) {
        assertThat(Arrays.asList(nonDesiredArray), everyItem(not(is(in(actualCollection)))));
    }

    private List<SortableIdentifier> extractMessageIdentifiersFromExpandedDoiRequest(
        ExpandedDoiRequest expandedDoiRequest) {
        return expandedDoiRequest
            .getDoiRequestMessages()
            .getMessages()
            .stream()
            .map(MessageDto::getMessageIdentifier)
            .collect(Collectors.toList());
    }

    private PublicationWithDoiRequestAndAllTypesOfMessages createSamplePublicationWithConversations()
        throws ApiGatewayException {
        return new PublicationWithDoiRequestAndAllTypesOfMessages(resourceService, doiRequestService, messageService)
            .create();
    }

    private Message sendSupportMessage(Publication createdPublication)
        throws TransactionFailedException, NotFoundException {
        UserInstance userInstance = UserInstance.fromPublication(createdPublication);
        SortableIdentifier identifier = messageService.createSimpleMessage(userInstance,
                                                                           createdPublication,
                                                                           randomString());
        return messageService.getMessage(userInstance, identifier);
    }

    private void initializeServices() {
        resourceService = new ResourceService(client, CLOCK);
        messageService = new MessageService(client, CLOCK);
        doiRequestService = new DoiRequestService(client, CLOCK);
        expansionService = new ResourceExpansionServiceImpl(resourceService,
                                                            messageService,
                                                            doiRequestService);
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

    private static class PublicationWithDoiRequestAndAllTypesOfMessages {

        private final ResourceService resourceService;
        private final DoiRequestService doiRequestService;
        private final MessageService messageService;
        private Publication publication;
        private DoiRequest doiRequest;
        private UserInstance userInstance;
        private List<Message> doiRequestMessages;
        private List<Message> supportMessages;

        public PublicationWithDoiRequestAndAllTypesOfMessages(
            ResourceService resourceService,
            DoiRequestService doiRequestService,
            MessageService messageService

        ) {

            this.resourceService = resourceService;
            this.doiRequestService = doiRequestService;
            this.messageService = messageService;
        }

        public DoiRequest getDoiRequest() {
            return doiRequest;
        }

        public List<Message> getDoiRequestMessages() {
            return doiRequestMessages;
        }

        public List<Message> getSupportMessages() {
            return supportMessages;
        }

        public Message getLastDoiRequestMessage() {
            return doiRequestMessages.get(doiRequestMessages.size() - 1);
        }

        public Message getLastSupportMessage() {
            return supportMessages.get(supportMessages.size() - 1);
        }

        public PublicationWithDoiRequestAndAllTypesOfMessages create() throws ApiGatewayException {
            var sample = PublicationGenerator.randomPublication();
            userInstance = UserInstance.fromPublication(sample);
            publication = resourceService.createPublication(userInstance, sample);

            var doiRequestIdentifier = doiRequestService.createDoiRequest(publication);
            doiRequest = doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);

            doiRequestMessages = createSampleDoiRequestMessages();
            supportMessages = createSampleSupportMessages();
            return this;
        }

        public SortableIdentifier[] getSupportMessageIdentifiers() {
            return getSupportMessages().stream().map(Message::getIdentifier).collect(Collectors.toList())
                .toArray(SortableIdentifier[]::new);
        }

        public SortableIdentifier[] getDoiRequestMessageIdentifiers() {
            return getDoiRequestMessages().stream().map(Message::getIdentifier).collect(Collectors.toList())
                .toArray(SortableIdentifier[]::new);
        }

        private List<Message> createSampleSupportMessages() {
            return smallStream()
                .map(ignored -> createSupportMessage())
                .collect(Collectors.toList());
        }

        private List<Message> createSampleDoiRequestMessages() {
            return smallStream()
                .map(ignored -> createDoiRequestMessage())
                .collect(Collectors.toList());
        }

        private Message createSupportMessage() {
            return attempt(() -> messageService.createSimpleMessage(userInstance, publication, randomString()))
                .map(messageIdentifier -> messageService.getMessage(userInstance, messageIdentifier))
                .orElseThrow();
        }

        private Message createDoiRequestMessage() {
            return attempt(() -> messageService.createDoiRequestMessage(userInstance, publication, randomString()))
                .map(messageIdentifier -> messageService.getMessage(userInstance, messageIdentifier))
                .orElseThrow();
        }

        private Stream<Integer> smallStream() {
            return IntStream.range(0, 2).boxed();
        }
    }
}
