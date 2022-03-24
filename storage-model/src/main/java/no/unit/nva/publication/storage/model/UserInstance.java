package no.unit.nva.publication.storage.model;

import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import nva.commons.core.JacocoGenerated;

/**
 * Class used internally in the Resource service to represent a user.
 */
public class UserInstance implements JsonSerializable {

    public static final URI UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI = null;
    private final URI organizationUri;
    private final String userIdentifier;
    private final URI topLevelOrgCristinId;

    protected UserInstance(String userIdentifier, URI organizationUri, URI topLevelOrgCristinId) {
        this.userIdentifier = userIdentifier;
        this.organizationUri = organizationUri;
        this.topLevelOrgCristinId = topLevelOrgCristinId;
    }

    public static UserInstance create(String userIdentifier, URI organizationUri) {
        return new UserInstance(userIdentifier, organizationUri, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI);
    }

    public static UserInstance create(ResourceOwner resourceOwner, URI organizationUri) {
        return new UserInstance(resourceOwner.getOwner(), organizationUri, resourceOwner.getOwnerAffiliation());
    }

    public static UserInstance fromDoiRequest(DoiRequest doiRequest) {
        return UserInstance.create(doiRequest.getOwner(), doiRequest.getCustomerId());
    }

    public static UserInstance fromPublication(Publication publication) {
        return UserInstance.create(publication.getResourceOwner(),publication.getPublisher().getId());
    }

    public static UserInstance fromMessage(Message message) {
        return UserInstance.create(message.getOwner(), message.getCustomerId());
    }

    public URI getOrganizationUri() {
        return organizationUri;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    @JacocoGenerated
    public URI getTopLevelOrgCristinId() {
        return topLevelOrgCristinId;
    }
}
