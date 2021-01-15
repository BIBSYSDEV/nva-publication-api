package no.unit.nva.publication.service.impl;

import java.net.URI;

/**
 *  Class used internally in the Resource service to represent a user.
 */
public class UserInstance {

    private final URI organizationUri;

    private final String userId;

    public UserInstance(String userId, URI organizationUri) {
        this.userId = userId;
        this.organizationUri = organizationUri;
    }

    public URI getOrganizationUri() {
        return organizationUri;
    }

    public String getUserId() {
        return userId;
    }
}
