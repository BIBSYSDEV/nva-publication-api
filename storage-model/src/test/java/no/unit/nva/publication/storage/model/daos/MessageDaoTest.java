package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MessageDaoTest extends ResourcesDynamoDbLocalTest {
    
    public static final URI SAMPLE_ORG = URI.create("https://example.org/123");
    public static final String SAMPLE_SENDER_USERNAME = "some@sender";
    public static final UserInstance SAMPLE_SENDER = new UserInstance(SAMPLE_SENDER_USERNAME, SAMPLE_ORG);
    public static final String SAMPLE_OWNER_USERNAME = "some@owner";
    public static final UserInstance SAMPLE_OWNER = new UserInstance(SAMPLE_OWNER_USERNAME, SAMPLE_ORG);
    public static final SortableIdentifier SAMPLE_RESOURCE_IDENTIFIER = SortableIdentifier.next();
    public static final String SAMPLE_TEXT = "some text";
    public static final Instant MESSAGE_CREATE_TIME = Instant.now();
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATE_TIME, Clock.systemDefaultZone().getZone());
    
    @BeforeEach
    public void initialize() {
        super.init();
    }
    
    @Test
    public void queryObjectCreatesObjectForRetrievingMessageByPrimaryKey() {
        Message message = insertSampleMessageInDatabase();
        MessageDao queryObject = MessageDao.queryObject(SAMPLE_OWNER, message.getIdentifier());
        Message retrievedMessage = fetchMessageFromDatabase(queryObject);
        
        assertThat(retrievedMessage, is(equalTo(message)));
    }
    
    private Message fetchMessageFromDatabase(MessageDao queryObject) {
        return attempt(() -> client.getItem(RESOURCES_TABLE_NAME, queryObject.primaryKey()))
                   .map(GetItemResult::getItem)
                   .map(item -> parseAttributeValuesMap(item, MessageDao.class))
                   .map(MessageDao::getData)
                   .orElseThrow();
    }
    
    private Message insertSampleMessageInDatabase() {
        Message message =
            Message.simpleMessage(SAMPLE_SENDER, SAMPLE_OWNER, SAMPLE_RESOURCE_IDENTIFIER, SAMPLE_TEXT, CLOCK);
        message.setIdentifier(SortableIdentifier.next());
        MessageDao dao = new MessageDao(message);
        client.putItem(RESOURCES_TABLE_NAME, dao.toDynamoFormat());
        return message;
    }
}