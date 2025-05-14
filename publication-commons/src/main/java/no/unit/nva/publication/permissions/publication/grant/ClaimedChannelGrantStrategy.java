package no.unit.nva.publication.permissions.publication.grant;

import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import java.net.URI;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public class ClaimedChannelGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public ClaimedChannelGrantStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        var claimedPublicationChannel = resource.getPrioritizedClaimedPublicationChannel();

        return claimedPublicationChannel.isPresent()
               && claimedPublicationChannelAllows(claimedPublicationChannel.get());
    }

    private boolean claimedPublicationChannelAllows(ClaimedPublicationChannel claimedPublicationChannel) {
        var channelOwner = claimedPublicationChannel.getCustomerId();
        var channelConstraint = claimedPublicationChannel.getConstraint();
        var editingPolicy = channelConstraint.editingPolicy();
        var publishingPolicy = channelConstraint.publishingPolicy();

        return hasOpenFiles()
                   ? channelPolicyAllows(editingPolicy, channelOwner)
                   : channelPolicyAllows(publishingPolicy, channelOwner);
    }

    private boolean hasOpenFiles() {
        return resource.getFiles().stream().anyMatch(OpenFile.class::isInstance);
    }

    private boolean channelPolicyAllows(ChannelPolicy policy, URI channelOwner) {
        if (OWNER_ONLY.equals(policy)) {
            return userInstance.getCustomerId().equals(channelOwner);
        } else if (EVERYONE.equals(policy)) {
            return userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
        }
        return false;
    }
}
