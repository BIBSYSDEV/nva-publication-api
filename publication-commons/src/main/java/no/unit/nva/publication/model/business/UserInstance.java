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
    private final URI personAffiliation;
    private final URI personCristinId;
    private final List<AccessRight> accessRights;
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")

    private UserClientType userClientType;

    public UserInstance(String userIdentifier, URI customerId, URI topLevelOrgCristinId, URI personAffiliation,
                        URI personCristinId,
                        List<AccessRight> accessRights, UserClientType userClientType) {
        this.user = new User(userIdentifier);
        this.customerId = customerId;
        this.topLevelOrgCristinId = topLevelOrgCristinId;
        this.personAffiliation = personAffiliation;
        this.personCristinId = personCristinId;
        this.accessRights = accessRights == null ? List.of() : accessRights;
        this.userClientType = userClientType;
    }

    public static UserInstance create(User user, URI customerId) {
        return new UserInstance(user.toString(), customerId, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI, null, null, null,
                                UserClientType.INTERNAL);
    }

    public static UserInstance create(String userIdentifier, URI customerId) {
        return new UserInstance(userIdentifier, customerId, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI, null, null, null,
                                UserClientType.INTERNAL);
    }

    public static UserInstance create(String userIdentifier, URI customerId, URI personCristinId,
                                      List<AccessRight> accessRights, URI topLevelOrgCristinId) {
        return new UserInstance(userIdentifier, customerId, topLevelOrgCristinId, null, personCristinId, accessRights,
                                UserClientType.INTERNAL);
    }

    public static UserInstance create(ResourceOwner resourceOwner, URI customerId) {
        return new UserInstance(resourceOwner.getOwner().getValue(), customerId,
                                resourceOwner.getOwnerAffiliation(), null, null, null, UserClientType.INTERNAL);
    }

    public static UserInstance createExternalUser(ResourceOwner resourceOwner, URI topLevelOrgCristinId) {
        var userInstance = create(resourceOwner, topLevelOrgCristinId);
        userInstance.userClientType = UserClientType.EXTERNAL;
        return userInstance;
    }

    public static UserInstance createBackendUser(ResourceOwner resourceOwner, URI topLevelOrgCristinId) {
        var userInstance = create(resourceOwner, topLevelOrgCristinId);
        userInstance.userClientType = UserClientType.BACKEND;
        return userInstance;
    }

    public boolean isExternalClient() {
        return this.userClientType.equals(UserClientType.EXTERNAL);
    }

    public boolean isBackendClient() {
        return this.userClientType.equals(UserClientType.BACKEND);
    }

    public static UserInstance fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        var userName = requestInfo.getUserName();
        var customerId = requestInfo.getCurrentCustomer();
        var personCristinId = attempt(requestInfo::getPersonCristinId).toOptional().orElse(null);
        var accessRights = requestInfo.getAccessRights();
        var topLevelOrgCristinId = requestInfo.getTopLevelOrgCristinId().orElse(null);
        var personAffiliation = attempt(requestInfo::getPersonAffiliation).orElse(failure -> null);
        return new UserInstance(userName, customerId, topLevelOrgCristinId, personAffiliation, personCristinId,
                                accessRights, UserClientType.INTERNAL);
    }

    public static UserInstance fromPublication(Publication publication) {
        return new UserInstance(publication.getResourceOwner().getOwner().getValue(),
                                publication.getPublisher().getId(),
                                publication.getResourceOwner().getOwnerAffiliation(),
                                null, null, List.of(), UserClientType.INTERNAL);
    }

    public static UserInstance fromMessage(Message message) {
        return UserInstance.create(message.getOwner(), message.getCustomerId());
    }

    public static UserInstance fromTicket(TicketEntry ticket) {
        return UserInstance.create(ticket.getOwner(), ticket.getCustomerId());
    }

    public boolean isSender(Message message) {
        return this.user.equals(message.getSender())
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

    public URI getPersonAffiliation() {
        return personAffiliation;
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

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserInstance that)) {
            return false;
        }
        return Objects.equals(getCustomerId(), that.getCustomerId()) &&
               Objects.equals(getUser(), that.getUser()) &&
               Objects.equals(getTopLevelOrgCristinId(), that.getTopLevelOrgCristinId()) &&
               Objects.equals(getPersonAffiliation(), that.getPersonAffiliation()) &&
               Objects.equals(getPersonCristinId(), that.getPersonCristinId()) &&
               Objects.equals(getAccessRights(), that.getAccessRights()) && userClientType == that.userClientType;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getCustomerId(), getUser(), getTopLevelOrgCristinId(), getPersonAffiliation(),
                            getPersonCristinId(), getAccessRights(), userClientType);
    }
}
