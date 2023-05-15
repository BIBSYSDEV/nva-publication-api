package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.time.Instant;
import java.util.UUID;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;

public final class StorageModelTestUtils {

    private StorageModelTestUtils() {

    }

    public static PublishingRequestCase randomPublishingRequest(Publication publication) {

        var userInstance = UserInstance.fromPublication(publication);
        var sample = new PublishingRequestCase();
        sample.setOwner(userInstance.getUser());
        sample.setCustomerId(userInstance.getOrganizationUri());
        sample.setIdentifier(SortableIdentifier.next());
        sample.setResourceIdentifier(SortableIdentifier.next());
        sample.setCreatedDate(randomInstant());
        sample.setModifiedDate(randomInstant());
        sample.setStatus(TicketStatus.COMPLETED);
        sample.setViewedBy(ViewedBy.addAll(sample.getOwner()));
        sample.setWorkflow(PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        sample.setAssignee(new Username(randomString()));
        sample.setFinalizedBy(new Username(randomString()));
        sample.setFinalizedDate(Instant.now());
        return sample;
    }

    public static PublishingRequestCase randomPublishingRequest() {
        var publication = randomPublication();
        return randomPublishingRequest(publication);
    }
}
