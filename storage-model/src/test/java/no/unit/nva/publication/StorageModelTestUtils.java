package no.unit.nva.publication;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static org.hamcrest.MatcherAssert.assertThat;
import com.github.javafaker.Faker;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;

public final class StorageModelTestUtils {

    private static Faker FAKER = Faker.instance();

    private StorageModelTestUtils() {

    }

    public static String randomString() {
        return FAKER.lorem().sentence();
    }

    public static PublishingRequestCase randomPublishingRequest(Publication publication) {

        var userInstance = UserInstance.fromPublication(publication);
        var sample = new PublishingRequestCase();
        sample.setOwner(userInstance.getUserIdentifier());
        sample.setCustomerId(userInstance.getOrganizationUri());
        sample.setResourceIdentifier(publication.getIdentifier());
        sample.setIdentifier(SortableIdentifier.next());
        sample.setRowVersion(UUID.randomUUID().toString());
        sample.setCreatedDate(randomInstant());
        sample.setModifiedDate(randomInstant());
        sample.setStatus(PublishingRequestStatus.APPROVED);
        assertThat(sample, doesNotHaveEmptyValues());
        return sample;
    }

    public static PublishingRequestCase randomPublishingRequest() {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        return randomPublishingRequest(publication);
    }
}
