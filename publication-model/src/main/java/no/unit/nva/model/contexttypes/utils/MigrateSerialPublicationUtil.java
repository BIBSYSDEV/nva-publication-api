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
    private static final int INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT = 2;

    @Deprecated
    public static URI migratePath(URI id, String pathToMigrate) {
        var oldPath = attempt(() -> UriWrapper.fromUri(id)
                                        .getPath()
                                        .getPathElementByIndexFromEnd(INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT))
                          .toOptional();
        return oldPath.filter(path -> path.equals(pathToMigrate))
                   .map(uri -> replaceOldPath(id))
                   .orElseGet(() -> isAlreadyMigrated(oldPath) ? id : logUnexpectedUri(id, pathToMigrate));
    }

    private static boolean isAlreadyMigrated(Optional<String> oldPath) {
        return oldPath.isPresent() && NEW_PATH_SERIAL_PUBLICATION.equals(oldPath.get());
    }

    private static URI logUnexpectedUri(URI id, String oldPath) {
        logger.info(String.format("Unexpected URI. Expected id with path: %s, but found id: %s", oldPath, id));
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
