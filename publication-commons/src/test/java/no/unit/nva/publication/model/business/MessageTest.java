package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import org.junit.jupiter.api.Test;

class MessageTest {
    
    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final UserInstance SAMPLE_OWNER = UserInstance.create("sample@owner", SOME_ORG);
    public static final ResourceOwner RANDOM_RESOURCE_OWNER = new ResourceOwner(SAMPLE_OWNER.getUserIdentifier(),
        SAMPLE_OWNER.getOrganizationUri());
    public static final String SOME_MESSAGE = "someMessage";
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
    public static final String MESSAGE_IDENTIFIER_FIELD = "identifier";
    private static final UserInstance SAMPLE_SENDER = sampleSender();
    
    @Test
    public void statusStringReturnsStringRepresentationOfStatus() {
        TicketStatus messageStatus = TicketStatus.READ;
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
        var message = createSampleMessage();
        String json = message.toString();
        Message recreatedMessage = dynamoDbObjectMapper.readValue(json, Message.class);
        assertThat(recreatedMessage, is(equalTo(message)));
    }
    
    @Test
    void simpleMessageReturnsMessageWithAllFieldsFieldInExceptForIdentifier() {
        var message = createSampleMessage();
        assertThat(message, doesNotHaveEmptyValuesIgnoringFields(Set.of(MESSAGE_IDENTIFIER_FIELD)));
    }
    
    private Message createSampleMessage() {
        Publication publication = randomPublication();
        SortableIdentifier messageIdentifier = SortableIdentifier.next();
        return Message.create(SAMPLE_SENDER, publication, SOME_MESSAGE, messageIdentifier, CLOCK,
            MessageType.SUPPORT);
    }
    
    @Test
    void shouldReturnCopyWithoutLossOfInformation() {
        Clock clock = Clock.systemDefaultZone();
        Publication publication = randomPublication();
        
        Message message = Message.create(SAMPLE_SENDER, publication, randomString(), SortableIdentifier.next(), clock,
            MessageType.SUPPORT);
        var copy = message.copy();
        assertThat(copy, doesNotHaveEmptyValues());
        assertThat(copy, is(equalTo(message)));
    }
    
    private static UserInstance sampleSender() {
        return UserInstance.create(SOME_SENDER, SOME_ORG);
    }
}