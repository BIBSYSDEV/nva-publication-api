package no.unit.nva.publication.events.handlers.persistence;

import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;

public final class PersistenceConfig {

    public static final UnixPath ANALYTICS_FOLDER = UnixPath.of("analytics", "publications");
    private static final Environment ENVIRONMENT = new Environment();
    public static final String PERSISTED_ENTRIES_BUCKET = ENVIRONMENT
        .readEnvOpt("PERSISTED_ENTRIES_BUCKET").orElse(null);

    private PersistenceConfig() {
    }
}
