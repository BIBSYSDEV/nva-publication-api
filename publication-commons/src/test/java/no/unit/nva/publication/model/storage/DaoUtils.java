package no.unit.nva.publication.model.storage;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.model.business.StorageModelTestUtils.randomPublishingRequest;
import static no.unit.nva.publication.model.storage.ResourceDao.CRISTIN_SOURCE;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
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
    
    public static Stream<Dao> instanceProvider() {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao.getData());
        MessageDao messageDao = sampleMessageDao();
        PublishingRequestDao approvePublicationRequestDao = sampleApprovePublicationRequestDao();
        return Stream.of(resourceDao, doiRequestDao, messageDao, approvePublicationRequestDao);
    }
    
    public static DoiRequestDao doiRequestDao(Entity resource) {
        
        return attempt(() -> DoiRequest.newDoiRequestForResource((Resource) resource))
            .map(DoiRequestDao::new)
            .orElseThrow();
    }
    
    static PutItemRequest toPutItemRequest(Dao resource) {
        return new PutItemRequest().withTableName(RESOURCES_TABLE_NAME)
            .withItem(resource.toDynamoFormat());
    }
    
    private static PublishingRequestDao sampleApprovePublicationRequestDao() {
        var publishingRequest = randomPublishingRequest().approve();
        return (PublishingRequestDao) publishingRequest.toDao();
    }
    
    private static MessageDao sampleMessageDao() {
        SortableIdentifier identifier = SortableIdentifier.next();
        UserInstance sender = UserInstance.create(randomString(), randomUri());
        Publication publication = PublicationGenerator.randomPublication();
        Message message = Message.create(sender, publication, randomString(), identifier, clock,
            MessageType.DOI_REQUEST);
        assertThat(message, doesNotHaveEmptyValues());
        return new MessageDao(message);
    }
}
