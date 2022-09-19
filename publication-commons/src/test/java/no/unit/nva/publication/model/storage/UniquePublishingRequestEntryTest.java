package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import org.junit.jupiter.api.Test;

class UniquePublishingRequestEntryTest {
    
    @Test
    void shouldContainPrimaryKeyThatEnsuresThatOnlyOnePublishingRequestExistsPerPublication() {
        var publication = randomPublication();
        var publishingRequest = PublishingRequestCase.createOpeningCaseObject(publication);
        var firstUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        var secondUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        assertThat(firstUniquenessEntry.primaryKey(), is(equalTo(secondUniquenessEntry.primaryKey())));
    }
}