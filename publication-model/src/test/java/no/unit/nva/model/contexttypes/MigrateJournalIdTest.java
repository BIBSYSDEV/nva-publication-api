package no.unit.nva.model.contexttypes;

import static no.unit.nva.utils.MigrateSerialPublicationsUtil.constructExampleIdWithPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;

@Deprecated
public class MigrateJournalIdTest {

    @Test
    void shouldUpdateOldJournalIdWithNewPathSerialPublication() {
        var oldId = constructExampleIdWithPath("journal");
        var expectedNewId = constructExampleIdWithPath("serial-publication");
        var journal = new Journal(oldId);
        assertThat(journal.getId(), is(equalTo(expectedNewId)));
    }
}
