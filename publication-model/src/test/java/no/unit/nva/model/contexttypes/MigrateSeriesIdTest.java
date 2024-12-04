package no.unit.nva.model.contexttypes;

import static no.unit.nva.utils.MigrateSerialPublicationsUtil.constructExampleIdWithPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

@Deprecated
public class MigrateSeriesIdTest {

    @Test
    void shouldUpdateOldSeriesIdWithNewPathSerialPublication() {
        var oldId = constructExampleIdWithPath("series");
        var expectedNewId = UriWrapper.fromUri(oldId)
                                .replacePathElementByIndexFromEnd(2, "serial-publication")
                                .getUri();
        var series = new Series(oldId);
        assertThat(series.getId(), is(equalTo(expectedNewId)));
    }
}
