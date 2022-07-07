package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.Test;

class UniquePublishingRequestEntryTest {

    @Test
    void shouldContainPrimaryKeyThatEnsuresThatOnlyOnePublishingRequestExistsPerPublication() {
        var userInstance = UserInstance.create(randomString(), randomUri());
        var publishingRequest = PublishingRequest.create(userInstance,
                                                         SortableIdentifier.next(),
                                                         SortableIdentifier.next(),
                                                         randomElement(PublishingRequestStatus.values()));
        var firstUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        var secondUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        assertThat(firstUniquenessEntry.primaryKey(), is(equalTo(secondUniquenessEntry.primaryKey())));
    }
}