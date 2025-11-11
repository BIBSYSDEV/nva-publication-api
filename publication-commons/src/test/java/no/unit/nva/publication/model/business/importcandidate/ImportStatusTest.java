package no.unit.nva.publication.model.business.importcandidate;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportStatusFactory;
import org.junit.jupiter.api.Test;

public class ImportStatusTest {

    @Test
    void shouldCreateCopyOfImportStatus() {
        var importStatus = ImportStatusFactory.createImported(randomString(), SortableIdentifier.next());
        var copy = importStatus.copy().build();
        assertThat(importStatus, is(equalTo(copy)));
    }
}
