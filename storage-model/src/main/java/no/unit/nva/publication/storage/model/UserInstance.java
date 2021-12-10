package no.unit.nva.publication.storage.model;

import java.net.URI;
import nva.commons.core.JsonSerializable;

/**
 *  Class used internally in the Resource service to represent a user.
 */
public class UserInstance implements JsonSerializable {

    private final URI organizationUri;
    private final String userIdentifier;

    public UserInstance(String userIdentifier, URI organizationUri) {
        this.userIdentifier = userIdentifier;
        this.organizationUri = organizationUri;
    }


    public URI getOrganizationUri() {
        return organizationUri;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }
}
