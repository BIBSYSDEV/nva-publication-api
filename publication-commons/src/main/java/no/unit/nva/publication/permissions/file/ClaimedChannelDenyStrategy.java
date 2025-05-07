package no.unit.nva.publication.permissions.file;

import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import java.util.List;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;

public class ClaimedChannelDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public ClaimedChannelDenyStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        return isWriteOrDelete(permission) && hasClaimedPublisher() && isDeniedUser();
    }

    private boolean isDeniedUser() {
        var organizationId = getClaimedPublisher().getFirst().getOrganizationId();
        return !organizationId.equals(userInstance.getTopLevelOrgCristinId());
    }

    private boolean hasClaimedPublisher() {
        return !getClaimedPublisher().isEmpty();
    }

    private List<ClaimedPublicationChannel> getClaimedPublisher() {
        return resource.getPublicationChannels()
                   .stream()
                   .filter(ClaimedPublicationChannel.class::isInstance)
                   .map(ClaimedPublicationChannel.class::cast)
                   .filter(channel -> PUBLISHER.equals(channel.getChannelType()))
                   .toList();
    }
}
