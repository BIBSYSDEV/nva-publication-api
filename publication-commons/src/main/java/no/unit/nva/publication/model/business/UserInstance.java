package no.unit.nva.publication.model.business;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
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
    private final UserClientType userClientType;
    private final ThirdPartySystem thirdPartySystem;

    public UserInstance(String userIdentifier, URI customerId, URI topLevelOrgCristinId, URI personAffiliation,
                        URI personCristinId,
                        List<AccessRight> accessRights, UserClientType userClientType, ThirdPartySystem thirdPartySystem) {
        this.user = new User(userIdentifier);
        this.customerId = customerId;
        this.topLevelOrgCristinId = topLevelOrgCristinId;
        this.personAffiliation = personAffiliation;
        this.personCristinId = personCristinId;
        this.accessRights = accessRights == null ? List.of() : accessRights;
        this.userClientType = userClientType;
        this.thirdPartySystem = thirdPartySystem;
    }

    public static UserInstance create(User user, URI customerId) {
        return new UserInstance(user.toString(), customerId, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI, null, null, null,
                                UserClientType.INTERNAL, null);
    }

    public static UserInstance create(String userIdentifier, URI customerId) {
        return new UserInstance(userIdentifier, customerId, UNDEFINED_TOP_LEVEL_ORG_CRISTIN_URI, null, null, null,
                                UserClientType.INTERNAL, null);
    }

    public static UserInstance create(String userIdentifier, URI customerId, URI personCristinId,
                                      List<AccessRight> accessRights, URI topLevelOrgCristinId) {
        return new UserInstance(userIdentifier, customerId, topLevelOrgCristinId, null, personCristinId, accessRights,
                                UserClientType.INTERNAL, null);
    }

    public static UserInstance create(ResourceOwner resourceOwner, URI customerId) {
        return create(resourceOwner, customerId, UserClientType.INTERNAL, null);
    }

    public static UserInstance create(ResourceOwner resourceOwner, URI customerId, UserClientType userClientType,
                                      ThirdPartySystem thirdPartySystem) {
        return new UserInstance(resourceOwner.getOwner().getValue(), customerId,
                                resourceOwner.getOwnerAffiliation(), null, null, null, userClientType, thirdPartySystem);
    }

    public static UserInstance createExternalUser(ResourceOwner resourceOwner, URI customerId, ThirdPartySystem thirdPartySystem) {
        return create(resourceOwner, customerId, UserClientType.EXTERNAL, thirdPartySystem);
    }

    public static UserInstance createBackendUser(ResourceOwner resourceOwner, URI customerId) {
        return create(resourceOwner, customerId, UserClientType.BACKEND, null);
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
                                accessRights, UserClientType.INTERNAL, null);
    }

    public static UserInstance fromResource(Resource resource) {
        return fromPublication(resource.toPublication());
    }

    public static UserInstance fromPublication(Publication publication) {
        return new UserInstance(Optional.ofNullable(publication.getResourceOwner()).map(ResourceOwner::getOwner).map(
            Username::getValue).orElse(null),
                                Optional.ofNullable(publication.getPublisher()).map(Organization::getId).orElse(null),
                                Optional.ofNullable(publication.getResourceOwner()).map(ResourceOwner::getOwnerAffiliation).orElse(null),
                                null, null, List.of(), UserClientType.INTERNAL, null);
    }

    public static UserInstance fromMessage(Message message) {
        return UserInstance.create(message.getOwner(), message.getCustomerId());
    }

    public static UserInstance fromTicket(TicketEntry ticket) {
        return new UserInstance(ticket.getOwner().toString(), ticket.getCustomerId(), ticket.getOwnerAffiliation(),
                                ticket.getResponsibilityArea(), null, List.of(), UserClientType.INTERNAL, null);
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

    public Optional<ThirdPartySystem> getThirdPartySystem() {
        return isExternalClient() ? Optional.ofNullable(thirdPartySystem) : Optional.empty();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserInstance that = (UserInstance) o;
        return Objects.equals(customerId, that.customerId)
               && Objects.equals(user, that.user)
               && Objects.equals(topLevelOrgCristinId, that.topLevelOrgCristinId)
               && Objects.equals(personAffiliation, that.personAffiliation)
               && Objects.equals(personCristinId, that.personCristinId)
               && Objects.equals(accessRights, that.accessRights)
               && userClientType == that.userClientType
               && thirdPartySystem == that.thirdPartySystem;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(customerId, user, topLevelOrgCristinId, personAffiliation, personCristinId, accessRights,
                            userClientType, thirdPartySystem);
    }
}
