package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.model.business.StorageModelTestUtils.randomPublishingRequest;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PublishingRequestCaseTest {

    public static final String FINALIZED_DATE = "finalizedDate";
    public static final String FINALIZED_BY = "finalizedBy";
    public static final String ASSIGNEE = "assignee";

    @Test
    void shouldReturnCopyWithoutInformationLoss() {
        var original = createSample(randomElement(TicketStatus.values()));
        var copy = original.copy();
        assertThat(original, doesNotHaveEmptyValuesIgnoringFields(Set.of(ASSIGNEE, FINALIZED_BY, FINALIZED_DATE)));
        assertThat(copy, is(equalTo(original)));
        assertThat(copy, is(not(sameInstance(original))));
    }
    
    private PublishingRequestCase createSample(TicketStatus status) {
        var sample = randomPublishingRequest();
        sample.setStatus(status);
        return sample;
    }
}