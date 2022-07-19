package no.unit.nva.publication.model.storage;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.UserInstance;
import org.junit.jupiter.api.Test;

class UniquePublishingRequestEntryTest {
    
    @Test
    void shouldContainPrimaryKeyThatEnsuresThatOnlyOnePublishingRequestExistsPerPublication() {
        var userInstance = UserInstance.create(randomString(), randomUri());
        var publishingRequest =
            PublishingRequestCase.createOpeningCaseObject(userInstance, SortableIdentifier.next());
        var firstUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        var secondUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        assertThat(firstUniquenessEntry.primaryKey(), is(equalTo(secondUniquenessEntry.primaryKey())));
    }
}