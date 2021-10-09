package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.publication.storage.model.MessageType;
import org.junit.jupiter.api.Test;

class MessageCollectionTest {

    private static final String MESSAGE_TYPE_FIELD = "messageType";

    @Test
    public void emptyReturnsAnEmptyMessageCollection() {
        var emptyCollection = MessageCollection.empty(MessageType.SUPPORT);
        assertThat(emptyCollection.getMessages(), is(empty()));
    }

    @Test
    public void messageCollectionMessageTypeIsSerializedAsTheSuppliedEnumValue() throws JsonProcessingException {
        var emptyCollection = MessageCollection.empty(MessageType.SUPPORT);
        String jsonString = dtoObjectMapper.writeValueAsString(emptyCollection);
        ObjectNode json = (ObjectNode) dtoObjectMapper.readTree(jsonString);

        String messageTypeValue = json.get(MESSAGE_TYPE_FIELD).textValue();
        assertThat(messageTypeValue, is(equalTo(MessageType.SUPPORT.getValue())));
    }
}