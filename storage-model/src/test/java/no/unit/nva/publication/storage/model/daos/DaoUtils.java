package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.StorageModelTestUtils.randomString;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.net.MalformedURLException;
import java.time.Clock;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceTest;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.WithIdentifier;
import nva.commons.core.attempt.Try;

public final class DaoUtils {

    private static final Clock clock = Clock.systemDefaultZone();
    public static final ResourceTest resourceGenerator = new ResourceTest();

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

    protected static <R extends WithIdentifier & RowLevelSecurity & DataEntry> PutItemRequest toPutItemRequest(
        Dao<R> resource) {
        return new PutItemRequest().withTableName(RESOURCES_TABLE_NAME)
            .withItem(resource.toDynamoFormat());
    }

    private static MessageDao sampleMessageDao() {
        SortableIdentifier identifier = SortableIdentifier.next();
        UserInstance sender = new UserInstance(randomString(), randomUri());
        Publication publication = PublicationGenerator.randomPublication();
        Message message = Message.doiRequestMessage(sender, publication, randomString(), identifier, clock);
        assertThat(message, doesNotHaveEmptyValues());
        return new MessageDao(message);
    }
}
