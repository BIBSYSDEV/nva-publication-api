package no.unit.nva.model.contexttypes.utils;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class MigrateSerialPublicationUtil {

    public static final Logger logger = LoggerFactory.getLogger(MigrateSerialPublicationUtil.class.getName());
    private static final String NEW_PATH_SERIAL_PUBLICATION = "serial-publication";
    private static final String OLD_PATH_JOURNAL = "journal";
    private static final String OLD_PATH_SERIES = "series";
    private static final int INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT = 2;

    @Deprecated
    public static URI migratePath(URI id) {
        var oldPath = attempt(() -> UriWrapper.fromUri(id)
                                        .getPath()
                                        .getPathElementByIndexFromEnd(INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT))
                          .toOptional();
        return oldPath.filter(MigrateSerialPublicationUtil::isDeprecated)
                   .map(uri -> replaceOldPath(id))
                   .orElseGet(() -> isAlreadyMigrated(oldPath) ? id : logUnexpectedUri(id));
    }

    private static boolean isDeprecated(String path) {
        return OLD_PATH_JOURNAL.equals(path) || OLD_PATH_SERIES.equals(path);
    }

    private static boolean isAlreadyMigrated(Optional<String> oldPath) {
        return oldPath.isPresent() && NEW_PATH_SERIAL_PUBLICATION.equals(oldPath.get());
    }

    private static URI logUnexpectedUri(URI id) {
        logger.info(String.format("Unexpected URI. Expected id with path: %s or %s, but found id: %s",
                                  OLD_PATH_JOURNAL, OLD_PATH_SERIES, id));
        return id;
    }

    @Deprecated
    private static URI replaceOldPath(URI id) {
        return UriWrapper.fromUri(id)
                   .replacePathElementByIndexFromEnd(INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT,
                                                     NEW_PATH_SERIAL_PUBLICATION)
                   .getUri();
    }
}
