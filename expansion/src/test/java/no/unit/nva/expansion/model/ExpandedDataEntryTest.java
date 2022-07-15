package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.IDENTIFIER_JSON_PTR;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.expansion.FakeResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDataEntryTest {
    
    public static final String TYPE = "type";
    public static final String EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY = "Publication";
    private static final MessageService messageService = new FakeMessageService();
    private static final ResourceExpansionService resourceExpansionService = new FakeResourceExpansionService();
    
    public static Stream<Class> entryProvider() {
        JsonSubTypes[] annotations = ExpandedDataEntry.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value);
    }
    
    @Test
    void shouldReturnExpandedResourceWithoutLossOfInformation() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var expandedResource = fromPublication(publication);
        var regeneratedPublication = objectMapper.readValue(expandedResource.toJsonString(), Publication.class);
        assertThat(regeneratedPublication, is(equalTo(publication)));
    }
    
    @Test
    void shouldReturnExpandedDoiRequestWithoutLossOfInformation() throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        var doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(publication));
        ExpandedDoiRequest expandedDoiRequest =
            ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService);
        assertThat(expandedDoiRequest.toDoiRequest(), is(equalTo(doiRequest)));
    }
    
    @Test
    void expandedResourceShouldHaveTypePublicationInheritingTheTypeFromThePublicationWhenItIsSerialized()
        throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var expandedResource = fromPublication(publication);
        var json = objectMapper.readTree(expandedResource.toJsonString());
        assertThat(json.get(TYPE).textValue(), is(equalTo(EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY)));
    }
    
    @Test
    void expandedDoiRequestShouldHaveTypeDoiRequest() throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        var doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(publication));
        var expandedResource =
            ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService);
        var json = objectMapper.convertValue(expandedResource, ObjectNode.class);
        assertThat(json.get(TYPE).textValue(), is(equalTo(ExpandedDoiRequest.TYPE)));
    }
    
    @ParameterizedTest(name = "should return identifier using a non serializable method:{0}")
    @MethodSource("entryProvider")
    void shouldReturnIdentifierUsingNonSerializableMethod(Class<?> type) {
        var expandedDataEntry = ExpandedDataEntryWithAssociatedPublication.create(type);
        SortableIdentifier identifier = expandedDataEntry.getExpandedDataEntry().identifyExpandedEntry();
        SortableIdentifier expectedIdentifier = extractExpectedIdentifier(expandedDataEntry);
        assertThat(identifier, is(equalTo(expectedIdentifier)));
    }
    
    private static ExpandedResourceConversation randomResourceConversation(Publication publication) {
        var expandedResourceConversation = new ExpandedResourceConversation();
        expandedResourceConversation.setPublicationSummary(PublicationSummary.create(publication));
        expandedResourceConversation.setPublicationIdentifier(publication.getIdentifier());
        return expandedResourceConversation;
    }
    
    private static ExpandedDoiRequest randomDoiRequest(Publication publication) {
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(publication));
        return attempt(() -> ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService))
            .orElseThrow();
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
        
        public ExpandedDataEntryWithAssociatedPublication(Publication publication, ExpandedDataEntry data) {
            this.publication = publication;
            this.expandedDataEntry = data;
        }
        
        public static ExpandedDataEntryWithAssociatedPublication create(Class<?> expandedDataEntryClass) {
            var publication = PublicationGenerator.randomPublication();
            if (expandedDataEntryClass.equals(ExpandedResource.class)) {
                return createExpandedResource(publication);
            } else if (expandedDataEntryClass.equals(ExpandedDoiRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(publication, randomDoiRequest(publication));
            } else if (expandedDataEntryClass.equals(ExpandedPublishingRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(publication,
                    createExpandedPublishingRequest(publication));
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
        
        private static ExpandedDataEntryWithAssociatedPublication createExpandedResource(
            Publication publication) {
            ExpandedResource expandedResource = attempt(() -> fromPublication(publication)).orElseThrow();
            return new ExpandedDataEntryWithAssociatedPublication(publication, expandedResource);
        }
        
        private static ExpandedDataEntry createExpandedPublishingRequest(Publication publication) {
            PublishingRequestCase requestCase = createRequestCase(publication);
            return ExpandedPublishingRequest.create(requestCase, messageService);
        }
        
        private static PublishingRequestCase createRequestCase(Publication publication) {
            var requestCase = new PublishingRequestCase();
            requestCase.setIdentifier(SortableIdentifier.next());
            requestCase.setStatus(PublishingRequestStatus.PENDING);
            requestCase.setModifiedDate(Instant.now());
            requestCase.setCreatedDate(Instant.now());
            requestCase.setRowVersion(UUID.randomUUID().toString());
            requestCase.setCustomerId(publication.getPublisher().getId());
            requestCase.setResourceIdentifier(publication.getIdentifier());
            requestCase.setOwner(publication.getResourceOwner().getOwner());
            return requestCase;
        }
    }
}