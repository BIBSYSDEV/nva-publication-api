package no.unit.nva.publication.permissions.publication.grant;

import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import java.net.URI;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaimedChannelGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public static final Logger logger = LoggerFactory.getLogger(ClaimedChannelGrantStrategy.class);

    public ClaimedChannelGrantStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        var claimedPublicationChannel = resource.getPrioritizedClaimedPublicationChannel();
        if (claimedPublicationChannel.isEmpty()) {
            return false;
        }

        var publicationChannel = claimedPublicationChannel.get();
        var channelOwner = publicationChannel.getCustomerId();
        var editingPolicy = publicationChannel.getConstraint().editingPolicy();
        var publishingPolicy = publicationChannel.getConstraint().publishingPolicy();

        if (hasOpenFiles()) {
            return channelPolicyAllows(editingPolicy, channelOwner);
        } else {
            return channelPolicyAllows(publishingPolicy, channelOwner);
        }
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
