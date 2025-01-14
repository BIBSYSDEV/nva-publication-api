package no.unit.nva.publication.permissions.file;

import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.UserInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FileStrategyBase {

    public static final Logger logger = LoggerFactory.getLogger(FileStrategyBase.class);

    protected final File file;
    protected final UserInstance userInstance;

    protected FileStrategyBase(File file, UserInstance userInstance) {
        this.file = file;
        this.userInstance = userInstance;
    }
}
