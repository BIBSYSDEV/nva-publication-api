package no.unit.nva.publication.storage.model;

import static no.unit.nva.model.testing.PublicationGenerator.publicationWithIdentifier;
import static no.unit.nva.publication.storage.model.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import org.junit.jupiter.api.Test;

public class MessageTypeTest {

    public static final String MESSAGE_TYPE_FIELD = "messageType";

    @Test
    void parseValueReturnsMessageTypeIgnoringInputCase() {
        var messageTypeInVaryingCase = "DoIReQuESt";
        var expectedMessageType = MessageType.DOI_REQUEST;
        var actualMessageType = MessageType.parse(messageTypeInVaryingCase);
        assertThat(actualMessageType, is(equalTo(expectedMessageType)));
    }

    @Test
    void parseValueThrowsExceptionWhenParsingInvalidMessageType() {
        var messageTypeString = "someInvalidType";
        var actualException =
            assertThrows(IllegalArgumentException.class, () -> MessageType.parse(messageTypeString));
        assertThat(actualException.getMessage(), containsString(MessageType.INVALID_MESSAGE_TYPE_ERROR));
    }

    @Test
    void parsingMessageValueFromJsonIsCaseTolerant() throws JsonProcessingException {
        Publication publication = publicationWithIdentifier();
        SortableIdentifier messageIdentifier = SortableIdentifier.next();
        UserInstance owner = UserInstance.create(publication.getOwner(), publication.getPublisher().getId());
        Message message = Message.create(owner, publication, randomString(), messageIdentifier,
                                         Clock.systemDefaultZone(), MessageType.DOI_REQUEST);

        ObjectNode json = dynamoDbObjectMapper.convertValue(message, ObjectNode.class);
        json.put(MESSAGE_TYPE_FIELD, "DoiREquEst");
        String jsonString = dynamoDbObjectMapper.writeValueAsString(json);
        Message actualMessage = dynamoDbObjectMapper.readValue(jsonString, Message.class);
        assertThat(actualMessage, is(equalTo(message)));
    }

    @Test
    void listGeneraSupportMessagesShouldExcludeOnlyDoiRequest() {
        assertThat(MessageType.generalSupportMessageTypes(), hasSize(MessageType.values().length - 1));
        assertThat(MessageType.generalSupportMessageTypes(), not(hasItem(MessageType.DOI_REQUEST)));
    }
}

