package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.PARTIAL_UPDATE;
import static no.unit.nva.model.PublicationOperation.UPLOAD_FILE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import java.net.URI;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
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

        if (PARTIAL_UPDATE.equals(permission) || UPLOAD_FILE.equals(permission)) {
            return PASS;
        }

        var claimedPublicationChannel = resource.getPrioritizedClaimedPublicationChannel();

        return claimedPublicationChannel.isPresent()
               && claimedPublicationChannelDenies(claimedPublicationChannel.get());
    }

    private boolean claimedPublicationChannelDenies(ClaimedPublicationChannel claimedPublicationChannel) {
        var channelConstraint = claimedPublicationChannel.getConstraint();
        var editingPolicy = channelConstraint.editingPolicy();
        var publishingPolicy = channelConstraint.publishingPolicy();
        var organization = claimedPublicationChannel.getOrganizationId();

        return hasOpenFiles()
                   ? channelPolicyDenies(editingPolicy, organization)
                   : channelPolicyDenies(publishingPolicy, organization);
    }

    private boolean hasOpenFiles() {
        return resource.getFiles().stream().anyMatch(OpenFile.class::isInstance);
    }

    private boolean channelPolicyDenies(ChannelPolicy policy, URI organizationId) {
        if (OWNER_ONLY.equals(policy)) {
            return !userInstance.getTopLevelOrgCristinId().equals(organizationId);
        } else if (EVERYONE.equals(policy)) {
            return !userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitutionOrChannelClaim();
        }
        return PASS;
    }
}
