package no.unit.nva.model.contexttypes;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.utils.MigrateSerialPublicationsUtil.constructExampleIdWithPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import no.unit.nva.model.contexttypes.utils.MigrateSerialPublicationUtil;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
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

    @Test
    void shouldUpdateOldSeriesIdWithNewPathSerialPublicationWhenOldPathIsJournal() {
        var oldId = constructExampleIdWithPath("journal");
        var expectedNewId = UriWrapper.fromUri(oldId)
                                .replacePathElementByIndexFromEnd(2, "serial-publication")
                                .getUri();
        var series = new Series(oldId);
        assertThat(series.getId(), is(equalTo(expectedNewId)));
    }

    @Test
    void shouldLogUriIfOldIdHasUnexpectedPath() {
        var oldId = constructExampleIdWithPath("unexpected-path");
        var appender = LogUtils.getTestingAppender(MigrateSerialPublicationUtil.class);

        new Series(oldId);

        assertThat(appender.getMessages(), containsString(oldId.toString()));
    }

    @Test
    void shouldLogUriIfOldIdHasUnexpectedForm() {
        var oldId = randomUri();
        var appender = LogUtils.getTestingAppender(MigrateSerialPublicationUtil.class);

        new Series(oldId);

        assertThat(appender.getMessages(), containsString(oldId.toString()));
    }

    @Test
    void shouldNotLogUriIfIdIsAlreadyMigrated() {
        var oldId = constructExampleIdWithPath("serial-publication");
        var appender = LogUtils.getTestingAppender(MigrateSerialPublicationUtil.class);

        new Series(oldId);

        assertThat(appender.getMessages(), not(containsString(oldId.toString())));
    }
}
