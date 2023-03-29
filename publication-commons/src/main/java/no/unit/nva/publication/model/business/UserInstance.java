package no.unit.nva.publication.model.business;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

/**
 * Class used internally in the Resource service to represent a user.
 */
public class UserInstance implements JsonSerializable {
    
    public static final URI UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI = null;
    private final URI organizationUri;
    private final User user;
    private final URI topLevelOrgCristinId;
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")

    private boolean isExternalClient;
    
    protected UserInstance(String userIdentifier, URI organizationUri, URI topLevelOrgCristinId) {
        this.user = new User(userIdentifier);
        this.organizationUri = organizationUri;
        this.topLevelOrgCristinId = topLevelOrgCristinId;
    }
    
    public static UserInstance create(User user, URI organizationUri) {
        return new UserInstance(user.toString(), organizationUri, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI);
    }
    
    public static UserInstance create(String userIdentifier, URI organizationUri) {
        return new UserInstance(userIdentifier, organizationUri, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI);
    }
    
    public static UserInstance create(ResourceOwner resourceOwner, URI organizationUri) {
        return new UserInstance(resourceOwner.getOwner(), organizationUri, resourceOwner.getOwnerAffiliation());
    }

    public static UserInstance createMachineUser(ResourceOwner resourceOwner, URI topLevelOrgCristinId) {
        var userInstance = create(resourceOwner, topLevelOrgCristinId);
        userInstance.isExternalClient = true;
        return userInstance;
    }

    public boolean isExternalClient() {
        return this.isExternalClient;
    }

    public static UserInstance fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        var userName = requestInfo.getNvaUsername();
        var customerId = requestInfo.getCurrentCustomer();
        return UserInstance.create(userName, customerId);
    }
    
    public static UserInstance fromDoiRequest(DoiRequest doiRequest) {
        return UserInstance.create(doiRequest.getOwner(), doiRequest.getCustomerId());
    }
    
    public static UserInstance fromPublication(Publication publication) {
        return UserInstance.create(publication.getResourceOwner(), publication.getPublisher().getId());
    }
    
    public static UserInstance fromMessage(Message message) {
        return UserInstance.create(message.getOwner(), message.getCustomerId());
    }
    
    public static UserInstance fromTicket(TicketEntry ticket) {
        return UserInstance.create(ticket.getOwner(), ticket.getCustomerId());
    }
    
    public URI getOrganizationUri() {
        return organizationUri;
    }
    
    public String getUsername() {
        return user.toString();
    }
    
    public User getUser() {
        return user;
    }
    
    @JacocoGenerated
    public URI getTopLevelOrgCristinId() {
        return topLevelOrgCristinId;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getOrganizationUri(), getUsername(), getTopLevelOrgCristinId());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserInstance)) {
            return false;
        }
        UserInstance that = (UserInstance) o;
        return Objects.equals(getOrganizationUri(), that.getOrganizationUri()) && Objects.equals(
            getUsername(), that.getUsername()) && Objects.equals(getTopLevelOrgCristinId(),
            that.getTopLevelOrgCristinId());
    }
}
