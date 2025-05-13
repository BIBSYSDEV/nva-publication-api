package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.PARTIAL_UPDATE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import java.util.Optional;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.permissions.publication.PublicationDenyStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public class ClaimedChannelDenyStrategy extends PublicationStrategyBase implements PublicationDenyStrategy {

    private static final boolean PASS = false;

    public ClaimedChannelDenyStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        if (isUsersDraft() || userInstance.isExternalClient() || userInstance.isBackendClient()) {
            return PASS;
        }

        return !PARTIAL_UPDATE.equals(permission) && hasClaimedPublisher() && isDeniedUser();
    }

    private boolean isDeniedUser() {
        return getClaimedPublisher().map(ClaimedPublicationChannel::getOrganizationId)
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
