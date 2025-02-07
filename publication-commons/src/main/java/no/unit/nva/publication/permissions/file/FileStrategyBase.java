package no.unit.nva.publication.permissions.file;

import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.UserInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStrategyBase {

    public static final Logger logger = LoggerFactory.getLogger(FileStrategyBase.class);

    protected final FileEntry file;
    protected final UserInstance userInstance;

    protected FileStrategyBase(FileEntry file, UserInstance userInstance) {
        this.file = file;
        this.userInstance = userInstance;
    }
}
