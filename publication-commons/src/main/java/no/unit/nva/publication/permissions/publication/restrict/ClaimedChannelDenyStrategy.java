package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.PARTIAL_UPDATE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import java.net.URI;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
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

        if (PARTIAL_UPDATE.equals(permission)) {
            return PASS;
        }

        var claimedPublicationChannel = resource.getPrioritizedClaimedPublicationChannel();
        if (claimedPublicationChannel.isEmpty()) {
            return false;
        }

        var publicationChannel = claimedPublicationChannel.get();
        var editingPolicy = publicationChannel.getConstraint().editingPolicy();
        var publishingPolicy = publicationChannel.getConstraint().publishingPolicy();
        var channelOwner = publicationChannel.getCustomerId();

        if (hasOpenFiles()) {
            return channelPolicyDenies(editingPolicy, channelOwner);
        } else {
            return channelPolicyDenies(publishingPolicy, channelOwner);
        }
    }

    private boolean channelPolicyDenies(ChannelPolicy policy, URI channelOwner) {
        if (OWNER_ONLY.equals(policy)) {
            return !userInstance.getCustomerId().equals(channelOwner);
        } else if (EVERYONE.equals(policy)) {
            return !userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
        }
        return false;
    }
}
