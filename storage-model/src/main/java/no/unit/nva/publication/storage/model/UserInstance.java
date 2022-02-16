package no.unit.nva.publication.storage.model;

import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;

/**
 * Class used internally in the Resource service to represent a user.
 */
public class UserInstance implements JsonSerializable {

    private final URI organizationUri;
    private final String userIdentifier;

    public UserInstance(String userIdentifier, URI organizationUri) {
        this.userIdentifier = userIdentifier;
        this.organizationUri = organizationUri;
    }

    public static UserInstance fromDoiRequest(DoiRequest doiRequest) {
        return new UserInstance(doiRequest.getOwner(), doiRequest.getCustomerId());
    }

    public static UserInstance fromPublication(Publication publication) {
        return new UserInstance(publication.getResourceOwner().getOwner(),publication.getPublisher().getId());
    }

    public URI getOrganizationUri() {
        return organizationUri;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }
}
