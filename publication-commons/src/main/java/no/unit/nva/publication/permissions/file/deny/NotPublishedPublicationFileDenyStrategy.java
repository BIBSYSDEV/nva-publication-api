package no.unit.nva.publication.permissions.file.deny;

import static java.util.Objects.isNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class NotPublishedPublicationFileDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public NotPublishedPublicationFileDenyStrategy(FileEntry file,
                                                   UserInstance userInstance,
                                                   Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        return isUnprivilegedUser() && !isPublicFile();
    }

    private boolean isPublicFile() {
        return isPublishedPublication() && file.getFile().isVisibleForNonOwner();
    }

    private boolean isPublishedPublication() {
        return PUBLISHED.equals(resource.getStatus());
    }

    private boolean isUnprivilegedUser() {
        return isNull(userInstance);
    }
}
