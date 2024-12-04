package no.unit.nva.model.contexttypes;

import static no.unit.nva.utils.MigrateSerialPublicationsUtil.constructExampleIdWithPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;

@Deprecated
public class MigrateSeriesIdTest {

    @Test
    void shouldUpdateOldSeriesIdWithNewPathSerialPublication() {
        var oldId = constructExampleIdWithPath("series");
        var expectedNewId = constructExampleIdWithPath("serial-publication");
        var series = new Series(oldId);
        assertThat(series.getId(), is(equalTo(expectedNewId)));
    }
}
