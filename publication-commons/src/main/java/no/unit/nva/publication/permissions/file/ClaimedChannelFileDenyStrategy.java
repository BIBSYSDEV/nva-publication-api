package no.unit.nva.publication.permissions.file;

import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import java.util.Optional;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;

public class ClaimedChannelFileDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public ClaimedChannelFileDenyStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        return isWriteOrDelete(permission)
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
