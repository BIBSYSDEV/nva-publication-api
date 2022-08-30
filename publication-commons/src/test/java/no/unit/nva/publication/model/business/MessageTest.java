package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.DaoUtils.randomTicketType;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.api.Test;

class MessageTest {
    
    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
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
    public void toStringReturnsAJsonString() throws JsonProcessingException, ConflictException {
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
    
    private Message createSampleMessage() throws ConflictException {
        var publication = randomPublicationEligibleForDoiRequest();
        var ticket = TicketEntry.createNewTicket(publication, DoiRequest.class, SortableIdentifier::next);
        return Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
    }
    
    private static Publication randomPublicationEligibleForDoiRequest() {
        return randomPublication().copy().withStatus(PublicationStatus.DRAFT).withDoi(null).build();
    }
    
    @Test
    void shouldReturnCopyWithoutLossOfInformation() throws ConflictException {
        Publication publication = randomPublicationEligibleForDoiRequest();
        var ticket = TicketEntry.createNewTicket(publication, randomTicketType(), SortableIdentifier::next);
        var message = Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        var copy = message.copy();
        assertThat(message, doesNotHaveEmptyValues());
        assertThat(copy, is(equalTo(message)));
    }
    
    private static UserInstance sampleSender() {
        return UserInstance.create(SOME_SENDER, SOME_ORG);
    }
}