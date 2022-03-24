package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.IDENTIFIER_JSON_PTR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import no.unit.nva.expansion.FakeResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDatabaseEntryTest {

    private static final ResourceExpansionService resourceExpansionService = new FakeResourceExpansionService();
    private static final MessageService messageService = mock(MessageService.class);

    public static Stream<ExpandedDataEntryWithAssociatedPublication> entryProvider()
        throws JsonProcessingException, NotFoundException {
        return Stream.of(ExpandedDataEntryWithAssociatedPublication.create(ExpandedResource.class),
                         ExpandedDataEntryWithAssociatedPublication.create(ExpandedDoiRequest.class),
                         ExpandedDataEntryWithAssociatedPublication.create(ExpandedResourceConversation.class)
        );
    }

    @ParameterizedTest(name = "should return identifier using a non serializable method")
    @MethodSource("entryProvider")
    void shouldReturnIdentifierUsingNonSerializableMethod(ExpandedDataEntryWithAssociatedPublication entry) {
        SortableIdentifier identifier = entry.getExpandedDataEntry().identifyExpandedEntry();
        SortableIdentifier expectedIdentifier = extractExpectedIdentifier(entry);
        assertThat(identifier, is(equalTo(expectedIdentifier)));
    }

    private static ExpandedDoiRequest randomDoiRequest(Publication publication) throws NotFoundException {
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(publication));
        return ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService);
    }

    private static ExpandedResourceConversation randomResourceConversation(Publication publication) {
        //TODO: create proper ExpandedResourceConversation
        var expandedResourceConversation = new ExpandedResourceConversation();
        expandedResourceConversation.setPublicationSummary(PublicationSummary.create(publication));
        expandedResourceConversation.setPublicationIdentifier(publication.getIdentifier());
        return expandedResourceConversation;
    }

    private SortableIdentifier extractExpectedIdentifier(ExpandedDataEntryWithAssociatedPublication generatedData) {
        if (entryIsAnAggregationOfOtherEntriesAndDoesNotHaveOwnIdentifiers(generatedData)) {
            return generatedData.getPublication().getIdentifier();
        } else {
            return new SortableIdentifier(extractIdFromSerializedObject(generatedData.getExpandedDataEntry()));
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

        public static ExpandedDataEntryWithAssociatedPublication create(Class<?> expandedDataEntryClass)
            throws JsonProcessingException, NotFoundException {
            var publication = PublicationGenerator.randomPublication();
            if (expandedDataEntryClass.equals(ExpandedResource.class)) {
                return
                    new ExpandedDataEntryWithAssociatedPublication(publication,
                                                                   ExpandedResource.fromPublication(publication));
            } else if (expandedDataEntryClass.equals(ExpandedDoiRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(publication, randomDoiRequest(publication));
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
    }
}