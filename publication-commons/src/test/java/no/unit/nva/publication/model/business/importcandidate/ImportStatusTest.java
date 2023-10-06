package no.unit.nva.publication.model.business.importcandidate;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import no.unit.nva.model.Username;
import org.junit.jupiter.api.Test;

public class ImportStatusTest {

    @Test
    void shouldCreateCopyOfImportStatus() {
        var importStatus = ImportStatusFactory.createImported(new Username(randomString()), randomUri());
        var copy = importStatus.copy().build();
        assertThat(importStatus, is(equalTo(copy)));
    }
}
