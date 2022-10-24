package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.ID_JSON_PTR;
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
import java.util.stream.Stream;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDataEntryTest extends ResourcesLocalTest {
    
    public static final String TYPE = "type";
    public static final String EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY = "Publication";
    
    private ResourceExpansionService resourceExpansionService;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;
    
    public static Stream<Class<?>> entryTypes() {
        return TypeProvider.listSubTypes(ExpandedDataEntry.class);
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        var clock = Clock.systemDefaultZone();
        this.resourceService = new ResourceService(client, clock);
        this.messageService = new MessageService(client);
        this.ticketService = new TicketService(client);
        this.resourceExpansionService =
            new ResourceExpansionServiceImpl(resourceService, ticketService);
    }
    
    @Test
    void shouldReturnExpandedResourceWithoutLossOfInformation() throws JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var expandedResource = fromPublication(publication);
        var regeneratedPublication = objectMapper.readValue(expandedResource.toJsonString(), Publication.class);
        assertThat(regeneratedPublication, is(equalTo(publication)));
    }
    
    @Test
    void expandedResourceShouldHaveTypePublicationInheritingTheTypeFromThePublicationWhenItIsSerialized()
        throws JsonProcessingException {
        var publication = randomPublication();
        var expandedResource = fromPublication(publication);
        var json = objectMapper.readTree(expandedResource.toJsonString());
        assertThat(json.get(TYPE).textValue(), is(equalTo(EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY)));
    }
    
    //TODO: parametrize test
    @Test
    void expandedDoiRequestShouldHaveTypeDoiRequest() throws ApiGatewayException {
        var publication = createPublicationWithoutDoi();
        var doiRequest = createDoiRequest(publication);
        var expandedResource =
            ExpandedDoiRequest.createEntry(doiRequest, resourceExpansionService, resourceService, ticketService);
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
                                                       ResourceService resourceService,
                                                       MessageService messageService,
                                                       TicketService ticketService)
        throws ApiGatewayException {
        var userInstance = UserInstance.fromPublication(publication);
        var doiRequest = (DoiRequest) TicketEntry.requestNewTicket(publication, DoiRequest.class)
                                          .persistNewTicket(ticketService);
        messageService.createMessage(doiRequest, userInstance, randomString());
        return attempt(() -> ExpandedDoiRequest.createEntry(doiRequest, resourceExpansionService,
            resourceService,
            ticketService))
                   .orElseThrow();
    }
    
    private static Publication randomPublicationWithoutDoi() {
        return randomPreFilledPublicationBuilder().withDoi(null).build();
    }
    
    private DoiRequest createDoiRequest(Publication publication) throws ApiGatewayException {
        return (DoiRequest) TicketEntry.requestNewTicket(publication, DoiRequest.class).persistNewTicket(ticketService);
    }
    
    private Publication createPublicationWithoutDoi() {
        var publication = randomPublicationWithoutDoi();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private SortableIdentifier extractExpectedIdentifier(ExpandedDataEntryWithAssociatedPublication generatedData) {
    
        ExpandedDataEntry expandedDataEntry = generatedData.getExpandedDataEntry();
        String identifier = extractIdFromSerializedObject(expandedDataEntry);
        return new SortableIdentifier(identifier);
    }
    
    private String extractIdFromSerializedObject(ExpandedDataEntry entry) {
        return Try.of(entry)
                   .map(ExpandedDataEntry::toJsonString)
                   .map(objectMapper::readTree)
                   .map(json -> (ObjectNode) json)
                   .map(json -> json.at(ID_JSON_PTR))
                   .map(JsonNode::textValue)
                   .map(UriWrapper::fromUri)
                   .map(UriWrapper::getLastPathElement)
                   .orElseThrow();
    }
    
    private static class ExpandedDataEntryWithAssociatedPublication {
        
        private final ExpandedDataEntry expandedDataEntry;
    
        public ExpandedDataEntryWithAssociatedPublication(ExpandedDataEntry data) {
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
                return new ExpandedDataEntryWithAssociatedPublication(randomDoiRequest(publication,
                    resourceExpansionService, resourceService, messageService, ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedPublishingRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(createExpandedPublishingRequest(publication,
                    resourceService,
                    resourceExpansionService,
                    ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedGeneralSupportRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(createExpandedGeneralSupportRequest(publication,
                    resourceService,
                    resourceExpansionService,
                    ticketService));
            } else {
                throw new UnsupportedOperationException();
            }
        }
    
        public ExpandedDataEntry getExpandedDataEntry() {
            return expandedDataEntry;
        }
    
        private static ExpandedDataEntry createExpandedGeneralSupportRequest(
            Publication publication,
            ResourceService resourceService,
            ResourceExpansionService resourceExpansionService,
            TicketService ticketService) throws NotFoundException {
            var request = (GeneralSupportRequest) GeneralSupportRequest.fromPublication(publication);
            return
                ExpandedGeneralSupportRequest.create(request, resourceService, resourceExpansionService, ticketService);
        }
    
        private static Publication createPublication(ResourceService resourceService) {
            var publication = randomPublicationWithoutDoi();
            publication = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
            return publication;
        }
    
        private static ExpandedDataEntryWithAssociatedPublication createExpandedResource(Publication publication) {
            ExpandedResource expandedResource = attempt(() -> fromPublication(publication)).orElseThrow();
            return new ExpandedDataEntryWithAssociatedPublication(expandedResource);
        }
    
        private static ExpandedDataEntry createExpandedPublishingRequest(
            Publication publication,
            ResourceService resourceService,
            ResourceExpansionService resourceExpansionService,
            TicketService ticketService) throws NotFoundException {
            PublishingRequestCase requestCase = createPublishingRequestCase(publication);
            return ExpandedPublishingRequest.create(requestCase,
                resourceService,
                resourceExpansionService,
                ticketService);
        }
    
        private static PublishingRequestCase createPublishingRequestCase(Publication publication) {
            var requestCase = new PublishingRequestCase();
            requestCase.setIdentifier(SortableIdentifier.next());
            requestCase.setStatus(TicketStatus.PENDING);
            requestCase.setModifiedDate(Instant.now());
            requestCase.setCreatedDate(Instant.now());
            requestCase.setCustomerId(publication.getPublisher().getId());
            requestCase.setPublicationDetails(PublicationDetails.create(publication));
            requestCase.setOwner(new User(publication.getResourceOwner().getOwner()));
            return requestCase;
        }
    }
}