package cucumber.permissions.publication;

import static cucumber.permissions.PermissionsRole.FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS;
import static cucumber.permissions.PermissionsRole.RELATED_EXTERNAL_CLIENT;
import static cucumber.permissions.RolesToAccessRights.roleToAccessRightsMap;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import cucumber.permissions.PermissionsRole;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.model.business.publicationchannel.ChannelType;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.AccessRight;

public class PublicationScenarioContext {

    private PublicationOperation operation;
    private PublicationStatus publicationStatus = PublicationStatus.PUBLISHED;
    private Set<PermissionsRole> roles = new HashSet<>();
    private boolean isDegree;
    private boolean hasClaimedPublisher;

    public void setOperation(PublicationOperation operation) {
        this.operation = operation;
    }

    public void setRoles(Set<PermissionsRole> roles) {
        this.roles = roles;
    }

    public void setPublicationStatus(PublicationStatus status) {
        this.publicationStatus = status;
    }

    public PublicationPermissions getPublicationPermissions() {
        var topLevelOrgCristinId = randomUri();
        var customerId = randomUri();

        var access = getAccessRights(roles);

        var user =
            roles.contains(RELATED_EXTERNAL_CLIENT) ?
                UserInstance.createExternalUser(new ResourceOwner(new Username(randomString()), randomUri()), customerId)
                : UserInstance.create(randomString(), customerId, randomUri(), access.stream().toList(), topLevelOrgCristinId);

        var currentUserIsContributor = roles.contains(PermissionsRole.OTHER_CONTRIBUTORS);
        var contributors = getContributors(user, currentUserIsContributor);

        var currentUserIsFileCurator = roles.contains(FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS);
        var curatingInstitutions = getCuratingInstitutions(currentUserIsFileCurator, user);

        var currentUserIsPublicationOwner = roles.contains(PermissionsRole.PUBLICATION_OWNER);
        var owner = getOwner(user, topLevelOrgCristinId, currentUserIsPublicationOwner);

        var randomResource =
            Resource.fromPublication(isDegree ? randomDegreePublication() : randomNonDegreePublication());

        var resource =
            randomResource.copy()
                .withPublisher(Organization.fromUri(roles.contains(RELATED_EXTERNAL_CLIENT) ? customerId : randomUri()))
                .withStatus(publicationStatus)
                .withResourceOwner(owner)
                .withCuratingInstitutions(curatingInstitutions)
                .withPublicationChannels(generatePublicationChannels(randomResource))
                .withEntityDescription(randomResource.getEntityDescription().copy()
                                           .withContributors(contributors)
                                           .build())
                .build();

        return new PublicationPermissions(resource.toPublication(), user);
    }

    private List<PublicationChannel> generatePublicationChannels(Resource randomResource) {
        return hasClaimedPublisher
                   ? List.of(randomClaimedChannel(randomResource.getIdentifier()))
                   : List.of();
    }

    private PublicationChannel randomClaimedChannel(SortableIdentifier resourceIdentifier) {
        return new ClaimedPublicationChannel(randomUri(), randomUri(), randomUri(),
                                             new Constraint(ChannelPolicy.EVERYONE, ChannelPolicy.OWNER_ONLY,
                                                            List.of()), ChannelType.PUBLISHER,
                                             SortableIdentifier.next(), resourceIdentifier, Instant.now(), Instant.now());
    }

    public void setIsDegree(boolean isDegree) {
        this.isDegree = isDegree;
    }

    public Set<PermissionsRole> getRoles() {
        return roles;
    }

    public void setHasClaimedPublisher(boolean hasClaimedPublisher) {
        this.hasClaimedPublisher = hasClaimedPublisher;
    }

    private static HashSet<CuratingInstitution> getCuratingInstitutions(boolean currentUserIsFileCurator,
                                                                        UserInstance user) {
        var curatingInstitutions = new HashSet<CuratingInstitution>();
        if (currentUserIsFileCurator) {
            curatingInstitutions.add(new CuratingInstitution(user.getTopLevelOrgCristinId(), Set.of()));
        }
        return curatingInstitutions;
    }

    private static Owner getOwner(UserInstance user, URI topLevelOrgCristinId, boolean currentUserIsOwner) {
        return currentUserIsOwner ? new Owner(user.getUser(), topLevelOrgCristinId)
                   : new Owner(new User(randomString()), topLevelOrgCristinId);
    }

    private static ArrayList<Contributor> getContributors(UserInstance user,
                                                          boolean addCurrentUserAsContributor) {
        var contributors = new ArrayList<Contributor>();
        if (addCurrentUserAsContributor) {
            contributors.add(new Contributor.Builder()
                                 .withAffiliations(List.of(Organization.fromUri(user.getTopLevelOrgCristinId())))
                                 .withIdentity(
                                     new Identity.Builder().withId(user.getPersonCristinId())
                                         .build())
                                 .withRole(new RoleType(Role.CREATOR)).build());
        }
        return contributors;
    }

    private static HashSet<AccessRight> getAccessRights(Set<PermissionsRole> roles) {
        return roleToAccessRightsMap.entrySet().stream()
                   .filter(entry -> roles.contains(entry.getKey()))
                   .map(Map.Entry::getValue)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toCollection(HashSet::new));
    }

    public PublicationOperation getOperation() {
        return operation;
    }
}
