package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.TestDataSource;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.api.Test;

class MessageTest extends TestDataSource {
    
    public static final String MESSAGE_IDENTIFIER_FIELD = "identifier";
    
    @Test
    void toPublicationThrowsUnsupportedException() {
        Message message = new Message();
        assertThrows(UnsupportedOperationException.class, () -> message.toPublication(null));
    }
    
    @Test
    void toStringReturnsAJsonString() throws JsonProcessingException, ConflictException {
        var message = createSampleMessage();
        String json = message.toString();
        Message recreatedMessage = dynamoDbObjectMapper.readValue(json, Message.class);
        assertThat(recreatedMessage, is(equalTo(message)));
    }
    
    @Test
    void simpleMessageReturnsMessageWithAllFieldsFieldInExceptForIdentifier() throws ConflictException {
        var message = createSampleMessage();
        assertThat(message, doesNotHaveEmptyValuesIgnoringFields(Set.of(MESSAGE_IDENTIFIER_FIELD)));
    }
    
    @Test
    void shouldReturnCopyWithoutLossOfInformation() {
        var publication = randomDegreePublicationEligibleForDoiRequest();
        var ticket = GeneralSupportRequest.create(Resource.fromPublication(publication),
                                                  UserInstance.fromPublication(publication));
        var message = Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        var copy = message.copy();
        assertThat(message, doesNotHaveEmptyValues());
        assertThat(copy, is(equalTo(message)));
    }

    @Test
    void shouldSerializeMessageWithStatusActiveWhenMessageStatusIsNull() throws JsonProcessingException {
        var message = "{\"type\":\"Message\"}";
        var serializedMessage = dynamoDbObjectMapper.readValue(message, Message.class);

        assertThat(serializedMessage.getStatus(), is(equalTo(MessageStatus.ACTIVE)));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNotSupportedMessageStatus() {
        assertThrows(IllegalArgumentException.class, () -> MessageStatus.lookup(randomString()));
    }
    
    private static Publication randomDegreePublicationEligibleForDoiRequest() {
        return randomDegreePublication().copy()
                   .withStatus(PublicationStatus.DRAFT).withDoi(null).build();
    }
    
    private Message createSampleMessage() throws ConflictException {
        var publication = randomDegreePublicationEligibleForDoiRequest();
        var ticket = TicketEntry.createNewTicket(publication, GeneralSupportRequest.class, SortableIdentifier::next);
        return Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
    }
}