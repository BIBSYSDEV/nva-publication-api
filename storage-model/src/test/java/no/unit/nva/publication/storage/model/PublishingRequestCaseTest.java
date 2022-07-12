package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

class PublishingRequestCaseTest {

    @Test
    void shouldReturnCopyWithoutInformationLoss() {
        var original = createSample(randomElement(PublishingRequestStatus.values()));
        var copy = original.copy();
        assertThat(original, doesNotHaveEmptyValues());
        assertThat(copy, is(equalTo(original)));
        assertThat(copy, is(not(sameInstance(original))));
    }

    @Test
    void shouldReturnCopyOfOriginalWithApprovedStatusButAllOtherFieldsTheSame() {
        var original = createSample(PublishingRequestStatus.PENDING);
        var approved = original.approve();
        var expected = original.copy();
        expected.setStatus(PublishingRequestStatus.APPROVED);
        assertThat(approved, is(equalTo(expected)));
    }

    private PublishingRequestCase createSample(PublishingRequestStatus pending) {
        var original = new PublishingRequestCase();
        original.setIdentifier(SortableIdentifier.next());
        original.setResourceIdentifier(SortableIdentifier.next());
        original.setStatus(pending);
        original.setOwner(randomString());
        original.setCustomerId(randomUri());
        original.setRowVersion(randomString());
        original.setModifiedDate(randomInstant());
        original.setCreatedDate(randomInstant());
        return original;
    }
}