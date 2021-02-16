package no.unit.nva.publication.storage.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class MessageTest {
    
    @Test
    public void statusStringReturnsStringRepresentationOfStatus() {
        MessageStatus messageStatus = MessageStatus.READ;
        Message message = Message.builder().withStatus(messageStatus).build();
        assertThat(message.getStatusString(), is(equalTo(messageStatus.toString())));
    }
    
    @Test
    public void toPublicationThrowsUnsupportedException() {
        Message message = new Message();
        assertThrows(UnsupportedOperationException.class, message::toPublication);
    }
    
    @Test
    public void toStringReturnsAJsonString() throws JsonProcessingException {
        Message message = Message.builder()
                              .withStatus(MessageStatus.UNREAD)
                              .withIdentifier(SortableIdentifier.next())
                              .build();
        String json = message.toString();
        Message recreatedMessage = JsonUtils.objectMapper.readValue(json, Message.class);
        assertThat(recreatedMessage, is(equalTo(message)));
    }
}