package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.StorageModelTestUtils.randomString;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.ResourceDao.CRISTIN_SOURCE;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.WithIdentifier;
import nva.commons.core.attempt.Try;

public final class DaoUtils {

    private static final Clock clock = Clock.systemDefaultZone();


    public static Resource sampleResource() {
        Publication publication = PublicationGenerator.randomPublication();
        var additionalIdentifier = Set.of(new AdditionalIdentifier(CRISTIN_SOURCE, randomString()));
        publication.setAdditionalIdentifiers(additionalIdentifier);
        return Resource.fromPublication(publication);

    }

    public static ResourceDao sampleResourceDao() {
        return Try.of(sampleResource())
            .map(ResourceDao::new)
            .orElseThrow();
    }

    public static Stream<Dao<?>> instanceProvider() {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao.getData());
        MessageDao messageDao = sampleMessageDao();
        PublishingRequestDao approvePublicationRequestDao = sampleApprovePublicationRequestDao();
        return Stream.of(resourceDao, doiRequestDao, messageDao, approvePublicationRequestDao);
    }

    private static PublishingRequestDao sampleApprovePublicationRequestDao() {
        var publishingRequest =
            PublishingRequest.newPublishingRequestResource(
                Resource.fromPublication(PublicationGenerator.randomPublication()));
        return new PublishingRequestDao(publishingRequest);
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
        UserInstance sender = UserInstance.create(randomString(), randomUri());
        Publication publication = PublicationGenerator.randomPublication();
        Message message = Message.doiRequestMessage(sender, publication, randomString(), identifier, clock);
        assertThat(message, doesNotHaveEmptyValues());
        return new MessageDao(message);
    }
}
