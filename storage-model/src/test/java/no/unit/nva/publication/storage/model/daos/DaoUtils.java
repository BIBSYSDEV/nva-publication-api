package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.StorageModelTestUtils.randomString;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceTest;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.WithIdentifier;
import nva.commons.core.attempt.Try;

public final class DaoUtils {

    public static final String SOME_OWNER = "some@owner";
    public static final String SOME_USER = "some@user";
    public static final URI SOME_CUSTOMER_ID = URI.create("https://some.example.org/123");
    public static final String SOME_TEXT = "someText";
    public static ResourceTest resourceGenerator = new ResourceTest();
    
    public static Resource sampleResource() throws InvalidIssnException, MalformedURLException {
        return Resource.fromPublication(resourceGenerator.samplePublication(
            resourceGenerator.sampleJournalArticleReference()));
    }
    
    public static ResourceDao sampleResourceDao() throws InvalidIssnException, MalformedURLException {
        return Try.of(sampleResource())
                   .map(ResourceDao::new)
            .orElseThrow();
    }

    
    public static Stream<Dao<?>> instanceProvider() throws InvalidIssnException, MalformedURLException {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao.getData());
        MessageDao messageDao = sampleMessageDao();
        return Stream.of(resourceDao, doiRequestDao, messageDao);
    }
    
    public static DoiRequestDao doiRequestDao(Resource resource) {
        return attempt(() -> DoiRequest.newDoiRequestForResource(resource))
                   .map(DoiRequestDao::new)
                   .orElseThrow();
    }
    
    protected static <R extends WithIdentifier & RowLevelSecurity & ResourceUpdate> PutItemRequest toPutItemRequest(
        Dao<R> resource) {
        return new PutItemRequest().withTableName(RESOURCES_TABLE_NAME)
                   .withItem(resource.toDynamoFormat());
    }
    
    private static MessageDao sampleMessageDao() {
        SortableIdentifier identifier = SortableIdentifier.next();
        Message message = Message.builder()
                              .withStatus(MessageStatus.UNREAD)
                              .withIdentifier(identifier)
                              .withOwner(SOME_OWNER)
                              .withSender(SOME_USER)
                              .withResourceIdentifier(SortableIdentifier.next())
                              .withResourceTitle(randomString())
                              .withCustomerId(SOME_CUSTOMER_ID)
                              .withDoiRequestRelated(true)
                              .withText(SOME_TEXT)
                              .withCreatedTime(Instant.now())
                              .build();
        assertThat(message, doesNotHaveEmptyValues());
        return new MessageDao(message);
    }
}
