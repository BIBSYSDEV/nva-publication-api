package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import org.junit.jupiter.api.Test;

class UniquePublishingRequestEntryTest {
    
    @Test
    void shouldContainPrimaryKeyThatEnsuresThatOnlyOnePublishingRequestExistsPerPublication() {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication);
        var userInstance = UserInstance.create(randomString(), randomUri());
        var publishingRequest = PublishingRequestCase.create(resource, userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var firstUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        var secondUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        assertThat(firstUniquenessEntry.primaryKey(), is(equalTo(secondUniquenessEntry.primaryKey())));
    }
}