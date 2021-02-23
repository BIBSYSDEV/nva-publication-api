package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.StorageModelTestUtils.randomString;
import static no.unit.nva.publication.storage.model.StorageModelConstants.PATH_SEPARATOR;
import static no.unit.nva.publication.storage.model.StorageModelConstants.URI_EMPTY_FRAGMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MessageTest {

    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final UserInstance SAMPLE_OWNER = new UserInstance("sample@owner", SOME_ORG);
    public static final String SOME_MESSAGE = "someMessage";
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
    public static final String MESSAGE_IDENTIFIER_FIELD = "identifier";
    public static final String API_HOST = "localhost";
    public static final String SCHEME = "https";
    public static final String MESSAGES_PATH = "/messages";
    private static final UserInstance SAMPLE_SENDER = sampleSender();

    @BeforeEach
    public void init() {
        Environment environment = setupEnvironment();
        StorageModelConstants.updateEnvironment(environment);
    }

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

    @Test
    public void simpleMessageReturnsMessageWithUriIdWhenMessageIdentifierIsNotNull() throws URISyntaxException {
        SortableIdentifier resourceIdentifier = SortableIdentifier.next();
        Publication publication = samplePublication(resourceIdentifier);
        SortableIdentifier messageIdentifier = SortableIdentifier.next();
        Message message = Message.simpleMessage(SAMPLE_SENDER, publication, SOME_MESSAGE, messageIdentifier, CLOCK);
        assertThat(message, doesNotHaveEmptyValues());
        String expectedPath = MESSAGES_PATH + PATH_SEPARATOR + messageIdentifier;
        URI expectedId = new URI(SCHEME, API_HOST, expectedPath, URI_EMPTY_FRAGMENT);
        assertThat(message.getId(), is(equalTo(expectedId)));
    }

    private static UserInstance sampleSender() {
        return new UserInstance(SOME_SENDER, SOME_ORG);
    }

    private Environment setupEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(StorageModelConstants.HOST_ENV_VARIABLE_NAME))
            .thenReturn(API_HOST);
        when(environment.readEnvOpt(StorageModelConstants.NETWORK_SCHEME_ENV_VARIABLE_NAME))
            .thenReturn(Optional.of(SCHEME));
        when(environment.readEnvOpt(StorageModelConstants.MESSAGES_PATH_ENV_VARIABLE_NAME))
            .thenReturn(Optional.of(MESSAGES_PATH));
        return environment;
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