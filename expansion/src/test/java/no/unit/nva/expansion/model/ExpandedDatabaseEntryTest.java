package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.IDENTIFIER_JSON_PTR;
import static no.unit.nva.publication.storage.model.Message.supportMessage;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.Clock;
import java.util.stream.Stream;
import no.unit.nva.expansion.FakeResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDatabaseEntryTest {

    private static final ResourceExpansionService resourceExpansionService = new FakeResourceExpansionService();

    public static Stream<ExpandedDataEntry> entryProvider() throws JsonProcessingException, NotFoundException {
        return Stream.of(randomResource(), randomDoiRequest(), randomMessage());
    }

    @ParameterizedTest(name = "should return identifier using a non serializable method")
    @MethodSource("entryProvider")
    void shouldReturnIdentifierUsingNonSerializableMethod(ExpandedDataEntry entry) {
        SortableIdentifier identifier = entry.retrieveIdentifier();
        SortableIdentifier fromSerializedId = SortableIdentifier.fromUri(extractIdFromSerializedObject(entry));
        assertThat(identifier, is(equalTo(fromSerializedId)));
    }

    private URI extractIdFromSerializedObject(ExpandedDataEntry entry) {
        return Try.of(entry)
            .map(ExpandedDataEntry::toJsonString)
            .map(objectMapper::readTree)
            .map(json -> (ObjectNode) json)
            .map(json -> json.at(IDENTIFIER_JSON_PTR))
            .map(JsonNode::textValue)
            .map(URI::create)
            .orElseThrow();
    }

    private static ExpandedResource randomResource() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        return ExpandedResource.fromPublication(publication);
    }

    private static ExpandedDoiRequest randomDoiRequest() throws NotFoundException {
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(
            Resource.fromPublication(PublicationGenerator.randomPublication()));
        return ExpandedDoiRequest.create(doiRequest, resourceExpansionService);
    }

    private static ExpandedMessage randomMessage() throws NotFoundException {
        var randomUser = new UserInstance(randomString(), randomUri());
        var publication = PublicationGenerator.randomPublication();
        var clock = Clock.systemDefaultZone();
        var message = supportMessage(randomUser, publication, randomString(), SortableIdentifier.next(), clock);
        return ExpandedMessage.create(message, resourceExpansionService);
    }
}