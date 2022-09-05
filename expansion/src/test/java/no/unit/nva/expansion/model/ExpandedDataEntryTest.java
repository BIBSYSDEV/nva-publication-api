package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.IDENTIFIER_JSON_PTR;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDataEntryTest extends ResourcesLocalTest {
    
    public static final String TYPE = "type";
    public static final String EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY = "Publication";
    private MessageService messageService = new FakeMessageService();
    private ResourceExpansionService resourceExpansionService;
    private ResourceService resourceService;
    private TicketService ticketService;
    
    public static Stream<Class<?>> entryTypes() {
        return TypeProvider.listSubTypes(ExpandedDataEntry.class);
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        var clock = Clock.systemDefaultZone();
        this.resourceService = new ResourceService(client, clock);
        this.messageService = new MessageService(client, clock);
        this.ticketService = new TicketService(client);
        this.resourceExpansionService =
            new ResourceExpansionServiceImpl(resourceService, messageService, ticketService);
    }
    
    @Test
    void shouldReturnExpandedResourceWithoutLossOfInformation() throws JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var expandedResource = fromPublication(publication);
        var regeneratedPublication = objectMapper.readValue(expandedResource.toJsonString(), Publication.class);
        assertThat(regeneratedPublication, is(equalTo(publication)));
    }
    
    @Test
    void shouldReturnExpandedDoiRequestWithoutLossOfInformation() throws ApiGatewayException {
        var publication = createPublicationWithoutDoi();
        var doiRequest = createDoiRequest(publication);
        ExpandedDoiRequest expandedDoiRequest =
            ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService);
        assertThat(expandedDoiRequest.toDoiRequest(), is(equalTo(doiRequest)));
    }
    
    @Test
    void expandedResourceShouldHaveTypePublicationInheritingTheTypeFromThePublicationWhenItIsSerialized()
        throws JsonProcessingException {
        var publication = randomPublication();
        var expandedResource = fromPublication(publication);
        var json = objectMapper.readTree(expandedResource.toJsonString());
        assertThat(json.get(TYPE).textValue(), is(equalTo(EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY)));
    }
    
    @Test
    void expandedDoiRequestShouldHaveTypeDoiRequest() throws ApiGatewayException {
        var publication = createPublicationWithoutDoi();
        var doiRequest = createDoiRequest(publication);
        var expandedResource =
            ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService);
        var json = objectMapper.convertValue(expandedResource, ObjectNode.class);
        assertThat(json.get(TYPE).textValue(), is(equalTo(ExpandedDoiRequest.TYPE)));
    }
    
    @ParameterizedTest(name = "should return identifier using a non serializable method:{0}")
    @MethodSource("entryTypes")
    void shouldReturnIdentifierUsingNonSerializableMethod(Class<?> type) throws ApiGatewayException {
        var expandedDataEntry =
            ExpandedDataEntryWithAssociatedPublication.create(type, resourceExpansionService,
                resourceService,
                messageService,
                ticketService);
        SortableIdentifier identifier = expandedDataEntry.getExpandedDataEntry().identifyExpandedEntry();
        SortableIdentifier expectedIdentifier = extractExpectedIdentifier(expandedDataEntry);
        assertThat(identifier, is(equalTo(expectedIdentifier)));
    }
    
    private static ExpandedDoiRequest randomDoiRequest(Publication publication,
                                                       ResourceExpansionService resourceExpansionService,
                                                       MessageService messageService,
                                                       TicketService ticketService)
        throws ApiGatewayException {
        var userInstance = UserInstance.fromPublication(publication);
        var doiRequest = DoiRequest.fromPublication(publication);
        var persistedDoiRequest = ticketService.createTicket(doiRequest, doiRequest.getClass());
        messageService.createMessage(userInstance, publication, randomString(), MessageType.DOI_REQUEST);
        return attempt(() -> ExpandedDoiRequest.create(persistedDoiRequest, resourceExpansionService, messageService))
                   .orElseThrow();
    }
    
    private static ExpandedResourceConversation randomResourceConversation(Publication publication) {
        var expandedResourceConversation = new ExpandedResourceConversation();
        expandedResourceConversation.setPublicationSummary(PublicationSummary.create(publication));
        expandedResourceConversation.setPublicationIdentifier(publication.getIdentifier());
        return expandedResourceConversation;
    }
    
    private static Publication randomPublicationWithoutDoi() {
        return randomPublication().copy().withDoi(null).build();
    }
    
    private DoiRequest createDoiRequest(Publication publication) throws ApiGatewayException {
        return ticketService.createTicket(DoiRequest.fromPublication(publication), DoiRequest.class);
    }
    
    private Publication createPublicationWithoutDoi() {
        var publication = randomPublicationWithoutDoi();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private SortableIdentifier extractExpectedIdentifier(ExpandedDataEntryWithAssociatedPublication generatedData) {
        if (entryIsAnAggregationOfOtherEntriesAndDoesNotHaveOwnIdentifiers(generatedData)) {
            return generatedData.getPublication().getIdentifier();
        } else {
            ExpandedDataEntry expandedDataEntry = generatedData.getExpandedDataEntry();
            String identifier = extractIdFromSerializedObject(expandedDataEntry);
            return new SortableIdentifier(identifier);
        }
    }
    
    private boolean entryIsAnAggregationOfOtherEntriesAndDoesNotHaveOwnIdentifiers(
        ExpandedDataEntryWithAssociatedPublication entry) {
        return entry.getExpandedDataEntry() instanceof ExpandedResourceConversation;
    }
    
    private String extractIdFromSerializedObject(ExpandedDataEntry entry) {
        return Try.of(entry)
                   .map(ExpandedDataEntry::toJsonString)
                   .map(objectMapper::readTree)
                   .map(json -> (ObjectNode) json)
                   .map(json -> json.at(IDENTIFIER_JSON_PTR))
                   .map(JsonNode::textValue)
                   .orElseThrow();
    }
    
    private static class ExpandedDataEntryWithAssociatedPublication {
        
        private final Publication publication;
        private final ExpandedDataEntry expandedDataEntry;
        
        public ExpandedDataEntryWithAssociatedPublication(Publication publication,
                                                          ExpandedDataEntry data) {
            this.publication = publication;
            this.expandedDataEntry = data;
        }
        
        public static ExpandedDataEntryWithAssociatedPublication create(
            Class<?> expandedDataEntryClass,
            ResourceExpansionService resourceExpansionService,
            ResourceService resourceService,
            MessageService messageService,
            TicketService ticketService) throws ApiGatewayException {
            var publication = createPublication(resourceService);
            if (expandedDataEntryClass.equals(ExpandedResource.class)) {
                return createExpandedResource(publication);
            } else if (expandedDataEntryClass.equals(ExpandedDoiRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(publication, randomDoiRequest(publication,
                    resourceExpansionService, messageService, ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedPublishingRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(publication,
                    createExpandedPublishingRequest(publication, resourceService, messageService,
                        resourceExpansionService));
            } else if (expandedDataEntryClass.equals(ExpandedGeneralSupportRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(publication,
                    createExpandedGeneralSupportRequest(publication, resourceService, resourceExpansionService));
            } else {
                return new ExpandedDataEntryWithAssociatedPublication(publication,
                    randomResourceConversation(publication));
            }
        }
    
        public Publication getPublication() {
            return publication;
        }
    
        public ExpandedDataEntry getExpandedDataEntry() {
            return expandedDataEntry;
        }
    
        private static ExpandedDataEntry createExpandedGeneralSupportRequest(
            Publication publication,
            ResourceService resourceService,
            ResourceExpansionService resourceExpansionService) throws NotFoundException {
            var request = (GeneralSupportRequest) GeneralSupportRequest.fromPublication(publication);
            return
                ExpandedGeneralSupportRequest.create(request, resourceService, resourceExpansionService);
        }
    
        private static Publication createPublication(ResourceService resourceService) {
            var publication = randomPublicationWithoutDoi();
            publication = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
            return publication;
        }
    
        private static ExpandedDataEntryWithAssociatedPublication createExpandedResource(Publication publication) {
            ExpandedResource expandedResource = attempt(() -> fromPublication(publication)).orElseThrow();
            return new ExpandedDataEntryWithAssociatedPublication(publication, expandedResource);
        }
    
        private static ExpandedDataEntry createExpandedPublishingRequest(
            Publication publication,
            ResourceService resourceService,
            MessageService messageService,
            ResourceExpansionService resourceExpansionService) throws NotFoundException {
            PublishingRequestCase requestCase = createPublishingRequestCase(publication);
            return ExpandedPublishingRequest.create(requestCase, resourceService, messageService,
                resourceExpansionService);
        }
    
        private static PublishingRequestCase createPublishingRequestCase(Publication publication) {
            var requestCase = new PublishingRequestCase();
            requestCase.setIdentifier(SortableIdentifier.next());
            requestCase.setStatus(TicketStatus.PENDING);
            requestCase.setModifiedDate(Instant.now());
            requestCase.setCreatedDate(Instant.now());
            requestCase.setVersion(UUID.randomUUID());
            requestCase.setCustomerId(publication.getPublisher().getId());
            requestCase.setResourceIdentifier(publication.getIdentifier());
            requestCase.setOwner(publication.getResourceOwner().getOwner());
            return requestCase;
        }
    }
}