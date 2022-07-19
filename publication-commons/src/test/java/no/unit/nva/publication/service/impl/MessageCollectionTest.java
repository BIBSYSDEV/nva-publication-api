package no.unit.nva.publication.service.impl;

import static com.spotify.hamcrest.jackson.IsJsonObject.jsonObject;
import static com.spotify.hamcrest.jackson.IsJsonText.jsonText;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonMissing;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.MessageCollection;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.Test;

class MessageCollectionTest {
    
    public static final Clock CLOCK = Clock.systemDefaultZone();
    public static final String TYPE_FIELD = "type";
    private static final String MESSAGE_TYPE_FIELD = "messageType";
    
    @Test
    void emptyReturnsAnEmptyMessageCollection() {
        var emptyCollection = MessageCollection.empty(MessageType.SUPPORT);
        assertThat(emptyCollection.getMessages(), is(empty()));
    }
    
    @Test
    void messageCollectionMessageTypeIsSerializedAsTheSuppliedEnumValue() throws JsonProcessingException {
        var emptyCollection = MessageCollection.empty(MessageType.SUPPORT);
        String jsonString = dtoObjectMapper.writeValueAsString(emptyCollection);
        ObjectNode json = (ObjectNode) dtoObjectMapper.readTree(jsonString);
        
        String messageTypeValue = json.get(MESSAGE_TYPE_FIELD).textValue();
        assertThat(messageTypeValue, is(equalTo(MessageType.SUPPORT.getValue())));
    }
    
    @Test
    void messageCollectionIsSerializedAndContainsAllMessagesAndMessageTypes() throws JsonProcessingException {
        var samplePublication = PublicationGenerator.randomPublication();
        var messageTexts = List.of(randomString(), randomString(), randomString());
        MessageCollection messageCollection = createSupportMessagesCollection(samplePublication, messageTexts);
        var serialization = dtoObjectMapper.writeValueAsString(messageCollection);
        var json = dtoObjectMapper.readTree(serialization);
        
        assertThat(json, is(jsonObject().where(TYPE_FIELD, is(jsonText(MessageCollection.TYPE_VALUE)))));
        var messageArray = (ArrayNode) json.get("messages");
        
        assertThat(messageArray.size(), is(equalTo(messageTexts.size())));
        var actualMessageTexts = extractTestFromActualMessages(messageArray);
        assertThat(actualMessageTexts, contains(messageTexts.toArray(String[]::new)));
    }
    
    @Test
    void messageCollectionPreservesInternalMessageStructureButDoesExposeItInSerialization()
        throws JsonProcessingException {
        var samplePublication = PublicationGenerator.randomPublication();
        var messageTexts = List.of(randomString(), randomString(), randomString());
        var messageCollection = createSupportMessagesCollection(samplePublication, messageTexts);
        var serialization = dtoObjectMapper.writeValueAsString(messageCollection);
        var json = dtoObjectMapper.readTree(serialization);
        
        var sampleMessage = messageCollection.getMessages().get(0);
        assertThat(sampleMessage, is(instanceOf(MessageDto.class)));
        assertThat(messageCollection.getMessagesInternalStructure().get(0), is(instanceOf(Message.class)));
        assertThat(json, is(jsonObject().where("messagesInternalStructure", is(jsonMissing()))));
    }
    
    private List<String> extractTestFromActualMessages(ArrayNode messageArray) {
        return Streams.stream(messageArray.elements())
            .map(message -> message.get(MessageDto.TEXT_FIELD))
            .map(JsonNode::textValue)
            .collect(Collectors.toList());
    }
    
    private MessageCollection createSupportMessagesCollection(Publication samplePublication,
                                                              List<String> messageTexts) {
        var messages = messageTexts.stream()
            .map(text -> createSupportMessage(samplePublication, text))
            .collect(Collectors.toList());
        return MessageCollection.groupMessagesByType(messages).stream()
            .collect(SingletonCollector.collect());
    }
    
    private Message createSupportMessage(Publication samplePublication, String text) {
        var userInstance = UserInstance.fromPublication(samplePublication);
        return Message.create(userInstance,
            samplePublication,
            text,
            SortableIdentifier.next(),
            CLOCK,
            MessageType.SUPPORT);
    }
}