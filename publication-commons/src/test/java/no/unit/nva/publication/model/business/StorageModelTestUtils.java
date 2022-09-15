package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static org.hamcrest.MatcherAssert.assertThat;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;

public final class StorageModelTestUtils {
    
    private StorageModelTestUtils() {
    
    }
    
    public static PublishingRequestCase randomPublishingRequest(Publication publication) {
        
        var userInstance = UserInstance.fromPublication(publication);
        var sample = new PublishingRequestCase();
        sample.setOwner(userInstance.getUserIdentifier());
        sample.setCustomerId(userInstance.getOrganizationUri());
        sample.setResourceIdentifier(publication.getIdentifier());
        sample.setIdentifier(SortableIdentifier.next());
        sample.setCreatedDate(randomInstant());
        sample.setModifiedDate(randomInstant());
        sample.setStatus(TicketStatus.COMPLETED);
        sample.setViewedBy(ViewedBy.addAll(sample.getOwner()));
        assertThat(sample, doesNotHaveEmptyValues());
        return sample;
    }
    
    public static PublishingRequestCase randomPublishingRequest() {
        var publication = randomPublication();
        return randomPublishingRequest(publication);
    }
}
