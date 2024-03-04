package no.unit.nva.publication.model.business;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

/**
 * Class used internally in the Resource service to represent a user.
 */
public class UserInstance implements JsonSerializable {

    public static final URI UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI = null;
    private final URI customerId;
    private final User user;
    private final URI topLevelOrgCristinId;
    private final URI personCristinId;
    private final List<AccessRight> accessRights;
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")

    private boolean isExternalClient;

    public UserInstance(String userIdentifier, URI customerId, URI topLevelOrgCristinId, URI personCristinId,
                   List<AccessRight> accessRights) {
        this.user = new User(userIdentifier);
        this.customerId = customerId;
        this.topLevelOrgCristinId = topLevelOrgCristinId;
        this.personCristinId = personCristinId;
        this.accessRights = accessRights == null ? List.of() : accessRights;
    }

    public static UserInstance create(User user, URI customerId) {
        return new UserInstance(user.toString(), customerId, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI, null, null);
    }

    public static UserInstance create(String userIdentifier, URI customerId) {
        return new UserInstance(userIdentifier, customerId, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI, null, null);
    }

    public static UserInstance create(String userIdentifier, URI customerId, URI personCristinId,
                                      List<AccessRight> accessRights) {
        return new UserInstance(userIdentifier, customerId, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI, personCristinId,
                                accessRights);
    }

    public static UserInstance create(ResourceOwner resourceOwner, URI customerId) {
        return new UserInstance(resourceOwner.getOwner().getValue(), customerId,
                                resourceOwner.getOwnerAffiliation(), null, null);
    }

    public static UserInstance createExternalUser(ResourceOwner resourceOwner, URI customerUri) {
        var userInstance = new UserInstance(resourceOwner.getOwner().getValue(), customerUri,
                                            resourceOwner.getOwnerAffiliation(), null, null);
        userInstance.isExternalClient = true;
        return userInstance;
    }

    public boolean isExternalClient() {
        return this.isExternalClient;
    }

    public static UserInstance fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        var userName = requestInfo.getUserName();
        var customerId = requestInfo.getCurrentCustomer();
        var personCristinId = attempt(requestInfo::getPersonCristinId).toOptional().orElse(null);
        var accessRights = requestInfo.getAccessRights();
        return UserInstance.create(userName, customerId, personCristinId, accessRights);
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

    public boolean isOwner(Message message) {
        return this.user.equals(message.getOwner())
               && this.customerId.equals(message.getCustomerId());
    }

    public URI getCustomerId() {
        return customerId;
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

    public URI getPersonCristinId() {
        return personCristinId;
    }

    public List<AccessRight> getAccessRights() {
        return accessRights;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getCustomerId(), getUsername(), getTopLevelOrgCristinId());
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
        return Objects.equals(getCustomerId(), that.getCustomerId()) && Objects.equals(
            getUsername(), that.getUsername()) && Objects.equals(getTopLevelOrgCristinId(),
                                                                 that.getTopLevelOrgCristinId());
    }
}
