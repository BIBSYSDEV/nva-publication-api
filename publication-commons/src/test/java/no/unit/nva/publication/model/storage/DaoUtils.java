package no.unit.nva.publication.model.storage;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.StorageModelTestUtils.randomPublishingRequest;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.TestDataSource;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.Named;

public final class DaoUtils extends TestDataSource {

    public static ResourceDao sampleResourceDao() {
        return Try.of(Resource.fromPublication(randomPublication()))
                   .map(ResourceDao::new)
                   .orElseThrow();
    }

    public static Stream<Dao> instanceProvider() {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao();
        MessageDao messageDao = sampleMessageDao();
        PublishingRequestDao approvePublicationRequestDao = sampleApprovePublishingRequestDao();
        FilesApprovalThesisDao filesApprovalThesisDao = sampleFilesApprovalThesisDao();
        return Stream.of(resourceDao, doiRequestDao, messageDao, approvePublicationRequestDao, filesApprovalThesisDao);
    }

    public static DoiRequestDao doiRequestDao() {
        var publication = randomPublicationEligibleForDoiRequest();
        var doiRequest = DoiRequest.create(Resource.fromPublication(publication),
                                           UserInstance.fromPublication(publication));
        return new DoiRequestDao(doiRequest);
    }

    public static DoiRequestDao doiRequestDao(ResourceDao resourceDao) {
        var resource = (Resource) resourceDao.getData();
        var userInstance = UserInstance.fromPublication(resource.toPublication());
        var doiRequest = DoiRequest.create(resource, userInstance);
        return new DoiRequestDao(doiRequest);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends TicketEntry> randomTicketType() {
        return (Class<? extends TicketEntry>)
                   randomElement(TypeProvider.listSubTypes(TicketEntry.class)
                                     .map(Named::getPayload)
                                     .toList());
    }

    static PutItemRequest toPutItemRequest(Dao resource) {
        return new PutItemRequest().withTableName(RESOURCES_TABLE_NAME)
                   .withItem(resource.toDynamoFormat());
    }

    private static FilesApprovalThesisDao sampleFilesApprovalThesisDao() {
        var publication = randomDegreePublication();
        var filesApprovalThesis = FilesApprovalThesis.createForUserInstitution(Resource.fromPublication(publication),
                                                                                  UserInstance.fromPublication(
                                                                                      publication),
                                                                                  PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        return (FilesApprovalThesisDao) filesApprovalThesis.toDao();
    }

    private static Publication randomPublicationEligibleForDoiRequest() {
        return randomDegreePublication().copy()
                   .withStatus(PublicationStatus.DRAFT)
                   .withDoi(null)
                   .build();
    }

    private static PublishingRequestDao sampleApprovePublishingRequestDao() {
        var publishingRequest = randomPublishingRequest().withOwner(randomString());
        return (PublishingRequestDao) publishingRequest.toDao();
    }

    private static MessageDao sampleMessageDao() {
        var publication = randomPublicationEligibleForDoiRequest();
        var ticket = randomTicket(publication);
        var message = Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        assertThat(message, doesNotHaveEmptyValues());
        return new MessageDao(message);
    }

    private static TicketEntry randomTicket(Publication publication) {
        return attempt(() -> TicketEntry.createNewTicket(publication, randomTicketType(), SortableIdentifier::next))
                   .map(ticketEntry -> ticketEntry.withOwner(randomString()))
                   .orElseThrow();
    }
}
