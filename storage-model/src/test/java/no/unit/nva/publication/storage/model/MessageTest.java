package no.unit.nva.publication.storage.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.jupiter.api.Test;

public class MessageTest {
    
    @Test
    public void statusStringReturnsStringRepresentationOfStatus() {
        MessageStatus messageStatus = MessageStatus.READ;
        Message message = Message.builder().withStatus(messageStatus).build();
        assertThat(message.getStatusString(), is(equalTo(messageStatus.toString())));
    }
}