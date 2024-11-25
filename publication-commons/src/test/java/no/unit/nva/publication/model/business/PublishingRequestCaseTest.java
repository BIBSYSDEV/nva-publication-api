package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.StorageModelTestUtils.randomPublishingRequest;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import org.junit.jupiter.api.Test;

class PublishingRequestCaseTest {

    @Test
    void shouldReturnCopyWithoutInformationLoss() {
        var original = createSample(randomElement(TicketStatus.values()));
        var copy = original.copy();
        assertThat(copy, is(equalTo(original)));
        assertThat(copy, is(not(sameInstance(original))));
    }

    @Test
    void shouldReturnTrueWhenApprovedFilesListContainsFileIdentifier() {
        var file = OpenFile.builder().withIdentifier(UUID.randomUUID()).buildOpenFile();
        var publishingRequestCase = createSample(randomElement(TicketStatus.values())).withFilesForApproval(
            Set.of(file)).approveFiles();

        assertTrue(publishingRequestCase.fileIsApproved(file));
    }

    private PublishingRequestCase createSample(TicketStatus status) {
        var sample = randomPublishingRequest();
        sample.setStatus(status);
        return sample;
    }
}