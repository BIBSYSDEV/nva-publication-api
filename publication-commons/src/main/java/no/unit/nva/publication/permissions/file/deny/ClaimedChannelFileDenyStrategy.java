package no.unit.nva.publication.permissions.file.deny;

import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class ClaimedChannelFileDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public ClaimedChannelFileDenyStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        if (currentUserIsFileOwner() && !fileIsFinalized()) {
            return false;
        }

        return isDeniedOperation(permission)
               && !isExternalClientWithRelation()
               && isDeniedUserByClaimedChannelWithinScope();
    }

    private boolean isDeniedOperation(FileOperation permission) {
        return resourceIsDegree() && fileHasEmbargo()
                   ? isWriteOrDeleteOrDownload(permission)
                   : isWriteOrDelete(permission);
    }

    private boolean isDeniedUserByClaimedChannelWithinScope() {
        return hasClaimedPublicationChannel() && !userBelongsToPublicationChannelOwner();
    }

    private boolean hasClaimedPublicationChannel() {
        return resource.getPrioritizedClaimedPublicationChannelWithinScope().isPresent();
    }
}
