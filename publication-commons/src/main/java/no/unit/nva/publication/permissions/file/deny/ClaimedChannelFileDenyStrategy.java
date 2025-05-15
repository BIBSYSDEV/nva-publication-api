package no.unit.nva.publication.permissions.file.deny;

import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import java.util.Optional;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class ClaimedChannelFileDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public ClaimedChannelFileDenyStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        return isWriteOrDelete(permission)
               && (file.getFile() instanceof OpenFile || file.getFile() instanceof InternalFile)
               && !isExternalClientWithRelation()
               && hasClaimedPublisher()
               && isDeniedUser();
    }

    private boolean isDeniedUser() {
        return getClaimedPublisher()
                    .map(ClaimedPublicationChannel::getOrganizationId)
                    .map(id -> !userInstance.getTopLevelOrgCristinId().equals(id))
                    .orElse(false);
    }

    private boolean hasClaimedPublisher() {
        return getClaimedPublisher().isPresent();
    }

    private Optional<ClaimedPublicationChannel> getClaimedPublisher() {
        return resource.getPublicationChannels()
                   .stream()
                   .filter(ClaimedPublicationChannel.class::isInstance)
                   .map(ClaimedPublicationChannel.class::cast)
                   .filter(channel -> PUBLISHER.equals(channel.getChannelType()))
                   .findFirst();
    }
}
