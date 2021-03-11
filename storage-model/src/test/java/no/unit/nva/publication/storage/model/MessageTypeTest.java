package no.unit.nva.publication.storage.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class MessageTypeTest {

    @Test
    void parseValueReturnsMessageTypeIgnoringInputCase() {
        var messageTypeString = "DoIReQuESt";
        var expectedMessageType = MessageType.DOI_REQUEST;
        var actualMessageType = MessageType.parse(messageTypeString);
        assertThat(actualMessageType, is(equalTo(expectedMessageType)));
    }

    @Test
    void parseValueThrowsExceptionWhenParsingInvalidMessageType() {
        var messageTypeString = "someInvalidType";
        var actualException =
            assertThrows(IllegalArgumentException.class, () -> MessageType.parse(messageTypeString));
        assertThat(actualException.getMessage(), containsString(MessageType.INVALID_MESSAGE_TYPE_ERROR));
    }
}