package no.unit.nva.expansion;

import static no.unit.nva.expansion.ResourceExpansionServiceImpl.UNSUPPORTED_TYPE;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
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
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedPublishingRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceConversation;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.model.MessageCollection;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.model.business.DataEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingRequestStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceExpansionServiceTest extends ResourcesLocalTest {
    
    public static final Clock CLOCK = Clock.systemDefaultZone();
    
    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private DoiRequestService doiRequestService;
    private PublishingRequestService publishingRequestService;
    
    public static Stream<Arguments> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class)
            .map(Arguments::arguments);
    }
    
    @BeforeEach
    void setUp() {
        super.init();
        initializeServices();
    }
    
    @Test
    void shouldReturnExpandedResourceConversationWithInstitutionsUriWhenPersonIsAffiliatedOnlyToInstitution()
        throws Exception {
        initializeServices();
        
        Publication createdPublication = createPublication();
        var expectedResourceOwnerAffiliation = createdPublication.getResourceOwner().getOwnerAffiliation();
        var message = sendSupportMessage(createdPublication);
        
        ExpandedResourceConversation expandedResourceConversation =
            (ExpandedResourceConversation) expansionService.expandEntry(message);
        assertThat(expandedResourceConversation.getOrganizationIds(),
            containsInAnyOrder(expectedResourceOwnerAffiliation));
    }
    
    @DisplayName("should return ticket with the affiliation that is contained in the publication.")
    @ParameterizedTest(name = "TicketType:{0}")
    @MethodSource("ticketTypeProvider")
    void shouldReturnExpandedTicketWithAffiliationContainedInCreatedPublication(
        Class<? extends ExpandedTicket> ticketType)
        throws Exception {
        var ticket = generateResourceUpdate(ticketType);
        var publication = ticket.getPublication();
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket.getDataEntry());
        var expectedOrgId = publication.getResourceOwner().getOwnerAffiliation();
        assertThat(expandedTicket.getOrganizationIds(), containsInAnyOrder(expectedOrgId));
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
    @MethodSource("fetchDataEntryTypes")
    void shouldProcessAllResourceUpdateTypes(Class<?> resourceUpdateType)
        throws IOException, ApiGatewayException {
        var resource = generateResourceUpdate(resourceUpdateType);
        expansionService.expandEntry(resource.getDataEntry());
        assertDoesNotThrow(() -> expansionService.expandEntry(resource.getDataEntry()));
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
    void shouldContainCorrectReferencesToReferencedPublicationWhenDoiRequestIsUpdated()
        throws ApiGatewayException, JsonProcessingException {
        var samplePublication =
            createSamplePublicationWithConversations();
        var expandedDoiRequestConversation =
            (ExpandedDoiRequest) expansionService.expandEntry(samplePublication.getLastDoiRequestMessage());
        
        var identifierInId =
            UriWrapper.fromUri(expandedDoiRequestConversation.getPublicationSummary().getPublicationId())
                .getLastPathElement();
        assertThat(expandedDoiRequestConversation.getPublicationSummary().getPublicationIdentifier(),
            is(equalTo(samplePublication.getPublicationIdentifier())));
        
        assertThat(identifierInId, is(equalTo(samplePublication.getPublicationIdentifier().toString())));
    }
    
    @Test
    void shouldIncludeSupportMessagesAndExcludeAllUnrelatedMessagesFromGeneralSupportConversations()
        throws ApiGatewayException, JsonProcessingException {
        var samplePublication = createSamplePublicationWithConversations();
        var expandedGeneralSupportConversation =
            (ExpandedResourceConversation) expansionService.expandEntry(samplePublication.getLastSupportMessage());
        
        assertThatSupportConversationIncludesSupportMessagesAndExcludesDoiRequestMessages(
            samplePublication, expandedGeneralSupportConversation);
    }
    
    @Test
    void shouldIncludeOnlyRelatedMessagesWhenExpandingPublishingRequest()
        throws ApiGatewayException, JsonProcessingException {
        var samplePublication = createSamplePublicationWithConversations();
        var expandedPublishingRequestEntry =
            (ExpandedPublishingRequest) expansionService.expandEntry(samplePublication.getPublishingRequest());
        assertThatMessageCollectionContainsExpectedMessages(expandedPublishingRequestEntry.getMessages(),
            samplePublication.getPublishingRequestMessages());
    }
    
    @Test
    void shouldExpandAssociatedPublishingRequestWhenNewPublishingRequestMessageIsPersisted()
        throws ApiGatewayException, JsonProcessingException {
        var samplePublication = createSamplePublicationWithConversations();
        var currentLastMessage = samplePublication.getLastPublishingRequestMessage();
        var actualExpandedEntry = (ExpandedPublishingRequest) expansionService.expandEntry(currentLastMessage);
        assertThatMessageCollectionContainsExpectedMessages(actualExpandedEntry.getMessages(),
            samplePublication.getPublishingRequestMessages());
    }
    
    @ParameterizedTest(name = "should add resource title to expanded ticket:{0}")
    @MethodSource("listTicketTypes")
    void shouldAddResourceTitleToExpandedEntry(Class<?> type) throws ApiGatewayException, JsonProcessingException {
        var resourceUpdate = generateResourceUpdate(type);
        var ticket = (ExpandedTicket) expansionService.expandEntry(resourceUpdate.getDataEntry());
        var expectedTitle = resourceUpdate.getPublication().getEntityDescription().getMainTitle();
        assertThat(ticket.getPublicationSummary().getTitle(), is(equalTo(expectedTitle)));
    }
    
    private static List<Class<?>> fetchDataEntryTypes() {
        var types = fetchDirectSubtypes(DataEntry.class);
        var nestedTypes = new Stack<Type>();
        var result = new ArrayList<Class<?>>();
        nestedTypes.addAll(types);
        while (!nestedTypes.isEmpty()) {
            var currentType = nestedTypes.pop();
            if (isTypeWithSubtypes(currentType)) {
                var subTypes = fetchDirectSubtypes(currentType.value());
                nestedTypes.addAll(subTypes);
            } else {
                result.add(currentType.value());
            }
        }
        return result;
    }
    
    private static boolean isTypeWithSubtypes(Type type) {
        return type.value().getAnnotationsByType(JsonSubTypes.class).length > 0;
    }
    
    private static List<Type> fetchDirectSubtypes(Class<?> type) {
        var annotations = type.getAnnotationsByType(JsonSubTypes.class);
        return Arrays.asList(annotations[0].value());
    }
    
    private static List<Class<?>> listTicketTypes() {
        JsonSubTypes[] annotations = TicketEntry.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value).collect(Collectors.toList());
    }
    
    private static List<Class<?>> listPublicationInstanceTypes() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes();
    }
    
    private void assertThatMessageCollectionContainsExpectedMessages(
        MessageCollection actualMessages,
        List<Message> expectedMessages) {
        var expectedMessageTexts =
            expectedMessages.stream().map(Message::getText).collect(Collectors.toList());
        var actualMessageTexts = actualMessages.getMessages()
            .stream()
            .map(MessageDto::getText)
            .collect(Collectors.toList());
        assertThat(actualMessageTexts, containsInAnyOrder(expectedMessageTexts.toArray(String[]::new)));
    }
    
    private void assertThatExpandedDoiRequestContainsOnlyDoiRequestMessagesAndNotAnyOfTheSupportMessages(
        PublicationWithAllKindsOfCasesAndMessages samplePublication,
        ExpandedDoiRequest expandedDoiRequestConversation) {
        var messagesIdentifiersInExpandedDoiRequest =
            extractMessageIdentifiersFromExpandedDoiRequest(expandedDoiRequestConversation);
        
        assertThat(messagesIdentifiersInExpandedDoiRequest,
            contains(samplePublication.getDoiRequestMessageIdentifiers()));
        assertThatNoItemInNonDesiredCollectionExistsInTheActualCollection(
            samplePublication.getSupportMessageIdentifiers(), messagesIdentifiersInExpandedDoiRequest);
    }
    
    private void assertThatSupportConversationIncludesSupportMessagesAndExcludesDoiRequestMessages(
        PublicationWithAllKindsOfCasesAndMessages samplePublication,
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
    
    private PublicationWithAllKindsOfCasesAndMessages createSamplePublicationWithConversations()
        throws ApiGatewayException {
        return new PublicationWithAllKindsOfCasesAndMessages(resourceService, doiRequestService, messageService,
            publishingRequestService)
            .create();
    }
    
    private Message sendSupportMessage(Publication createdPublication)
        throws NotFoundException {
        UserInstance userInstance = UserInstance.fromPublication(createdPublication);
        
        SortableIdentifier identifier = messageService.createMessage(userInstance, createdPublication,
            randomString(), MessageType.SUPPORT);
        return messageService.getMessage(userInstance, identifier);
    }
    
    private void initializeServices() {
        resourceService = new ResourceService(client, CLOCK);
        messageService = new MessageService(client, CLOCK);
        doiRequestService = new DoiRequestService(client, CLOCK);
        publishingRequestService = new PublishingRequestService(client, CLOCK);
        expansionService = new ResourceExpansionServiceImpl(resourceService, messageService,
            doiRequestService, publishingRequestService);
    }
    
    private Publication createPublication() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        return resourceService.createPublication(userInstance, publication);
    }
    
    private DataEntryWithAssociatedPublication generateResourceUpdate(Class<?> resourceUpdateType)
        throws ApiGatewayException {
        Publication createdPublication = createPublication();
        
        if (Resource.class.equals(resourceUpdateType)) {
            var resource = Resource.fromPublication(createdPublication);
            return new DataEntryWithAssociatedPublication(resource, createdPublication);
        }
        if (DoiRequest.class.equals(resourceUpdateType)) {
            return new DataEntryWithAssociatedPublication(createDoiRequest(createdPublication), createdPublication);
        }
        if (Message.class.equals(resourceUpdateType)) {
            var message = sendSupportMessage(createdPublication);
            return new DataEntryWithAssociatedPublication(message, createdPublication);
        }
        if (PublishingRequestCase.class.equals(resourceUpdateType)) {
            var request = createPublishingRequest(createdPublication);
            return new DataEntryWithAssociatedPublication(request, createdPublication);
        }
        
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + resourceUpdateType.getSimpleName());
    }
    
    private DataEntry createPublishingRequest(Publication createdPublication) {
        var publishingRequest = new PublishingRequestCase();
        publishingRequest.setStatus(PublishingRequestStatus.PENDING);
        publishingRequest.setCustomerId(createdPublication.getPublisher().getId());
        publishingRequest.setModifiedDate(Instant.now());
        publishingRequest.setCreatedDate(Instant.now());
        publishingRequest.setResourceIdentifier(createdPublication.getIdentifier());
        publishingRequest.setOwner(createdPublication.getResourceOwner().getOwner());
        publishingRequest.setRowVersion(UUID.randomUUID().toString());
        publishingRequest.setIdentifier(SortableIdentifier.next());
        assertThat(publishingRequest, doesNotHaveEmptyValues());
        return publishingRequest;
    }
    
    private DoiRequest createDoiRequest(Publication createdPublication) {
        Resource resource = Resource.fromPublication(createdPublication);
        
        return DoiRequest.newDoiRequestForResource(resource);
    }
    
    private static class DataEntryWithAssociatedPublication {
        
        private final DataEntry dataEntry;
        private final Publication publication;
        
        public DataEntryWithAssociatedPublication(DataEntry dataEntry, Publication publication) {
            this.dataEntry = dataEntry;
            this.publication = publication;
        }
        
        public DataEntry getDataEntry() {
            return dataEntry;
        }
        
        public Publication getPublication() {
            return publication;
        }
    }
    
    private static class PublicationWithAllKindsOfCasesAndMessages {
        
        private final ResourceService resourceService;
        private final DoiRequestService doiRequestService;
        private final MessageService messageService;
        private PublishingRequestService publishingRequestService;
        private Publication publication;
        private DoiRequest doiRequest;
        private UserInstance userInstance;
        private List<Message> doiRequestMessages;
        private List<Message> supportMessages;
        private List<Message> publishingRequestMessages;
        private PublishingRequestCase publishingRequest;
        
        public PublicationWithAllKindsOfCasesAndMessages(
            ResourceService resourceService,
            DoiRequestService doiRequestService,
            MessageService messageService,
            PublishingRequestService publishingRequestService
        
        ) {
            
            this.resourceService = resourceService;
            this.doiRequestService = doiRequestService;
            this.messageService = messageService;
            this.publishingRequestService = publishingRequestService;
        }
        
        public PublishingRequestCase getPublishingRequest() {
            return publishingRequest;
        }
        
        public List<Message> getPublishingRequestMessages() {
            return publishingRequestMessages;
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
        
        public Message getLastPublishingRequestMessage() {
            return publishingRequestMessages.get(publishingRequestMessages.size() - 1);
        }
        
        public SortableIdentifier getPublicationIdentifier() {
            return publication.getIdentifier();
        }
        
        public PublicationWithAllKindsOfCasesAndMessages create() throws ApiGatewayException {
            var sample = PublicationGenerator.randomPublication();
            userInstance = UserInstance.fromPublication(sample);
            publication = resourceService.createPublication(userInstance, sample);
            
            var doiRequestIdentifier = doiRequestService.createDoiRequest(publication);
            doiRequest = doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);
            
            var publishingRequestCase =
                PublishingRequestCase.createOpeningCaseObject(userInstance, publication.getIdentifier());
            publishingRequest = publishingRequestService.createPublishingRequest(publishingRequestCase);
            doiRequestMessages = createSomeMessages(MessageType.DOI_REQUEST);
            supportMessages = createSomeMessages(MessageType.SUPPORT);
            publishingRequestMessages = createSomeMessages(MessageType.PUBLISHING_REQUEST);
            
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
        
        private List<Message> createSomeMessages(MessageType messageType) {
            return smallStream()
                .map(ignored -> createMessage(messageType))
                .collect(Collectors.toList());
        }
        
        private Message createMessage(MessageType support) {
            return attempt(
                () -> messageService.createMessage(userInstance, publication, randomString(), support))
                .map(messageIdentifier -> messageService.getMessage(userInstance, messageIdentifier))
                .orElseThrow();
        }
        
        private Stream<Integer> smallStream() {
            return IntStream.range(0, 2).boxed();
        }
    }
}
