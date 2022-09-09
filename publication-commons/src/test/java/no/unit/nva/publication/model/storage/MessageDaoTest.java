package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageDaoTest extends ResourcesLocalTest {
    
    public static final URI SAMPLE_ORG = URI.create("https://example.org/123");
    public static final String SAMPLE_SENDER_USERNAME = "some@sender";
    public static final UserInstance SAMPLE_SENDER = UserInstance.create(SAMPLE_SENDER_USERNAME, SAMPLE_ORG);
    public static final String SAMPLE_OWNER_USERNAME = "some@owner";
    public static final UserInstance SAMPLE_OWNER = UserInstance.create(SAMPLE_OWNER_USERNAME, SAMPLE_ORG);
    public static final ResourceOwner RANDOM_RESOURCE_OWNER = new ResourceOwner(SAMPLE_OWNER.getUserIdentifier(),
        SAMPLE_OWNER.getOrganizationUri());
    public static final SortableIdentifier SAMPLE_RESOURCE_IDENTIFIER = SortableIdentifier.next();
    public static final String SAMPLE_TEXT = "some text";
    public static final Instant MESSAGE_CREATE_TIME = Instant.now();
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATE_TIME, Clock.systemDefaultZone().getZone());
    private MessageService messageService;
    
    @BeforeEach
    public void initialize() {
        super.init();
        this.messageService = new MessageService(client, Clock.systemDefaultZone());
    }
    
    @Test
    void shouldBeRetrievableByIdentifier() {
        var message = insertSampleMessageInDatabase();
        var retrievedMessage = messageService.getMessageByIdentifier(message.getIdentifier()).orElseThrow();
        assertThat(retrievedMessage, is(equalTo(message)));
    }
    
    @Test
    void queryObjectCreatesObjectForRetrievingMessageByPrimaryKey() {
        var message = insertSampleMessageInDatabase();
        var queryObject = MessageDao.queryObject(SAMPLE_OWNER, message.getIdentifier());
        var retrievedMessage = fetchMessageFromDatabase(queryObject);
        
        assertThat(retrievedMessage, is(equalTo(message)));
    }
    
    @Test
    void listMessagesForCustomerAndStatusReturnsObjectWithCustomerIdAndStatus() {
        var expectedMessageStatus = MessageStatus.READ;
        var expectedUri = URI.create("https://example.com");
        var queryObject = MessageDao.listMessagesForCustomerAndStatus(expectedUri, expectedMessageStatus);
        assertThat(queryObject.getCustomerId(), is(equalTo(expectedUri)));
        assertThat(queryObject.getData().getStatus(), is(equalTo(expectedMessageStatus)));
    }
    
    @Test
    void listMessagesAndResourcesForUserReturnsDaoWithOwnerAndPublisher() {
        var actualMessage = MessageDao.listMessagesAndResourcesForUser(SAMPLE_OWNER);
        assertThat(actualMessage.getOwner(), is(equalTo(SAMPLE_OWNER.getUserIdentifier())));
        assertThat(actualMessage.getCustomerId(), is(equalTo(SAMPLE_OWNER.getOrganizationUri())));
    }
    
    private Message fetchMessageFromDatabase(MessageDao queryObject) {
        return attempt(() -> client.getItem(RESOURCES_TABLE_NAME, queryObject.primaryKey()))
            .map(GetItemResult::getItem)
            .map(item -> parseAttributeValuesMap(item, MessageDao.class))
            .map(MessageDao::getData)
            .orElseThrow();
    }
    
    private Message insertSampleMessageInDatabase() {
        Organization publisher = new Builder().withId(SAMPLE_OWNER.getOrganizationUri()).build();
        Publication publication = new Publication.Builder()
            .withResourceOwner(RANDOM_RESOURCE_OWNER)
            .withIdentifier(SAMPLE_RESOURCE_IDENTIFIER)
            .withPublisher(publisher)
            .build();
        Message message =
            Message.create(SAMPLE_SENDER,
                publication,
                SAMPLE_TEXT,
                SortableIdentifier.next(),
                CLOCK,
                MessageType.SUPPORT);
        MessageDao dao = new MessageDao(message);
        client.putItem(RESOURCES_TABLE_NAME, dao.toDynamoFormat());
        return message;
    }
}