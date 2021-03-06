package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.StorageModelTestUtils.randomString;
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
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class MessageTest {

    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final UserInstance SAMPLE_OWNER = new UserInstance("sample@owner", SOME_ORG);
    public static final String SOME_MESSAGE = "someMessage";
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
    public static final String MESSAGE_IDENTIFIER_FIELD = "identifier";
    private static final UserInstance SAMPLE_SENDER = sampleSender();

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

    @Test
    public void doiRequestMessageReturnsMessageRelatedToResourceAndNotRelatedToDoiRequest() {
        SortableIdentifier resourceIdentifier = SortableIdentifier.next();
        Publication publication = samplePublication(resourceIdentifier);
        SortableIdentifier messageIdentifier = SortableIdentifier.next();

        Message message = Message.doiRequestMessage(SAMPLE_SENDER, publication, SOME_MESSAGE, messageIdentifier, CLOCK);
        assertThat(message.isDoiRequestRelated(), is(equalTo(true)));
        assertThat(message.getResourceIdentifier(), is(equalTo(resourceIdentifier)));
    }

    @Test
    public void simpleMessageReturnsMessageRelatedToResourceAndNotRelatedToDoiRequest() {
        SortableIdentifier resourceIdentifier = SortableIdentifier.next();
        Publication publication = samplePublication(resourceIdentifier);
        SortableIdentifier messageIdentifier = SortableIdentifier.next();

        Message message = Message.simpleMessage(SAMPLE_SENDER, publication, SOME_MESSAGE, messageIdentifier, CLOCK);
        assertThat(message.isDoiRequestRelated(), is(equalTo(false)));
        assertThat(message.getResourceIdentifier(), is(equalTo(resourceIdentifier)));
    }

    @Test
    public void simpleMessageReturnsMessageWithAllFieldsFieldInExceptForIdentifier() {
        SortableIdentifier resourceIdentifier = SortableIdentifier.next();
        Publication publication = samplePublication(resourceIdentifier);
        SortableIdentifier messageIdentifier = SortableIdentifier.next();
        Message message = Message.simpleMessage(SAMPLE_SENDER, publication, SOME_MESSAGE, messageIdentifier, CLOCK);
        assertThat(message, doesNotHaveEmptyValuesIgnoringFields(Set.of(MESSAGE_IDENTIFIER_FIELD)));
    }

    private static UserInstance sampleSender() {
        return new UserInstance(SOME_SENDER, SOME_ORG);
    }

    private Publication samplePublication(SortableIdentifier resourceIdentifier) {
        Organization publisher = new Builder().withId(SAMPLE_OWNER.getOrganizationUri()).build();
        EntityDescription entityDescription = new EntityDescription.Builder().withMainTitle(randomString()).build();
        return new Publication.Builder()
                   .withPublisher(publisher)
                   .withOwner(SAMPLE_OWNER.getUserIdentifier())
                   .withIdentifier(resourceIdentifier)
                   .withEntityDescription(entityDescription)
                   .build();
    }
}