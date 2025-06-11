package cucumber.permissions.publication;

import static cucumber.permissions.PermissionsRole.AUTHENTICATED_BUT_NO_ACCESS;
import static cucumber.permissions.PermissionsRole.CREATOR;
import static cucumber.permissions.PermissionsRole.NOT_RELATED_EXTERNAL_CLIENT;
import static cucumber.permissions.PermissionsRole.RELATED_EXTERNAL_CLIENT;
import static cucumber.permissions.PermissionsRole.UNAUTHENTICATED;
import static cucumber.permissions.RolesToAccessRights.roleToAccessRightsMap;
import static cucumber.permissions.enums.UserInstitutionConfig.BELONGS_TO_CREATING_INSTITUTION;
import static cucumber.permissions.enums.UserInstitutionConfig.BELONGS_TO_CURATING_INSTITUTION;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomFinalizedFiles;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomNonFinalizedFiles;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import cucumber.permissions.PermissionsRole;
import cucumber.permissions.enums.ChannelClaimConfig;
import cucumber.permissions.enums.FileConfig;
import cucumber.permissions.enums.PublicationTypeConfig;
import cucumber.permissions.enums.UserInstitutionConfig;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
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

    public static final URI CREATING_INSTITUTION = randomUri();
    public static final URI CURATING_INSTITUTION = randomUri();
    public static final URI NON_CURATING_INSTITUTION = randomUri();
    public static final String USER_NAME = randomString();
    private static final URI USER_CRISTIN_ID = randomUri();
    private static final List<String> DEGREE_SCOPE = List.of("DegreeLicentiate",
                                                             "DegreeBachelor",
                                                             "DegreeMaster",
                                                             "DegreePhd",
                                                             "ArtisticDegreePhd",
                                                             "OtherStudentWork");
    private static final String INSPERA = "inspera";

    private PublicationTypeConfig publicationTypeConfig = PublicationTypeConfig.PUBLICATION;
    private boolean isImportedDegree;
    private PublicationStatus publicationStatus = PublicationStatus.PUBLISHED;
    private FileConfig fileConfig = FileConfig.NO_FILES;
    private ChannelClaimConfig channelClaimConfig = ChannelClaimConfig.NON_CLAIMED;
    private ChannelPolicy channelClaimPublishingPolicy;
    private ChannelPolicy channelClaimEditingPolicy;
    private Set<PermissionsRole> roles = new HashSet<>();
    private UserInstitutionConfig userInstitutionConfig = BELONGS_TO_CREATING_INSTITUTION;
    private PublicationOperation operation;

    public PublicationTypeConfig getPublicationTypeConfig() {
        return publicationTypeConfig;
    }

    public void setPublicationTypeConfig(
        PublicationTypeConfig publicationTypeConfig) {
        this.publicationTypeConfig = publicationTypeConfig;
    }

    public boolean isImportedDegree() {
        return isImportedDegree;
    }

    public void setIsImportedDegree(boolean importedDegree) {
        isImportedDegree = importedDegree;
    }

    public PublicationStatus getPublicationStatus() {
        return publicationStatus;
    }

    public FileConfig getFileConfig() {
        return fileConfig;
    }

    public void setFileConfig(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
    }

    public void setPublicationStatus(PublicationStatus status) {
        this.publicationStatus = status;
    }

    public ChannelClaimConfig getChannelClaimConfig() {
        return channelClaimConfig;
    }

    public void setChannelClaimConfig(ChannelClaimConfig channelClaimConfig) {
        this.channelClaimConfig = channelClaimConfig;
    }

    public ChannelPolicy getChannelClaimPublishingPolicy() {
        return nonNull(channelClaimPublishingPolicy) ? channelClaimPublishingPolicy : ChannelPolicy.EVERYONE;
    }

    public void setChannelClaimPublishingPolicy(
        ChannelPolicy channelClaimPublishingPolicy) {
        this.channelClaimPublishingPolicy = channelClaimPublishingPolicy;
    }

    public ChannelPolicy getChannelClaimEditingPolicy() {
        return nonNull(channelClaimEditingPolicy) ? channelClaimEditingPolicy : ChannelPolicy.OWNER_ONLY;
    }

    public void setChannelClaimEditingPolicy(
        ChannelPolicy channelClaimEditingPolicy) {
        this.channelClaimEditingPolicy = channelClaimEditingPolicy;
    }

    public Set<PermissionsRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<PermissionsRole> roles) {
        this.roles = roles;
    }

    public UserInstitutionConfig getUserInstitutionConfig() {
        return userInstitutionConfig;
    }

    public void setUserInstitutionConfig(
        UserInstitutionConfig userInstitutionConfig) {
        this.userInstitutionConfig = userInstitutionConfig;
    }

    public PublicationOperation getOperation() {
        return operation;
    }

    public void setOperation(PublicationOperation operation) {
        this.operation = operation;
    }

    public PublicationPermissions getPublicationPermissions() {
        var resource = createResource();
        var userInstance = getUserInstance();

        return new PublicationPermissions(resource, userInstance);
    }

    public Resource createResource() {
        var publication = PublicationTypeConfig.DEGREE.equals(getPublicationTypeConfig())
                              ? randomDegreePublication()
                              : randomNonDegreePublication();
        var resource = Resource.fromPublication(publication);

        return resource.copy()
                   .withStatus(getPublicationStatus())
                   .withPublisher(Organization.fromUri(CREATING_INSTITUTION))
                   .withResourceOwner(createOwner())
                   .withCuratingInstitutions(createCuratingInstitutions())
                   .withPublicationChannels(generatePublicationChannelsForPublisher(resource))
                   .withEntityDescription(createEntityDescription(resource))
                   .withAdditionalIdentifiers(addAdditionalIdentifiers(resource))
                   .withAssociatedArtifactsList(createAssociatedArtifacts())
                   .build();
    }

    // TODO: Edit so it is only instantiated once
    public UserInstance getUserInstance() {
        if (roles.contains(UNAUTHENTICATED)) {
            return null;
        }
        if (roles.contains(AUTHENTICATED_BUT_NO_ACCESS)) {
            return UserInstance.create(USER_NAME, NON_CURATING_INSTITUTION, USER_CRISTIN_ID, List.of(),
                                       NON_CURATING_INSTITUTION);
        }
        if (roles.contains(RELATED_EXTERNAL_CLIENT)) {
            return UserInstance.createExternalUser(
                new ResourceOwner(new Username(USER_NAME), CREATING_INSTITUTION), CREATING_INSTITUTION);
        }
        if (roles.contains(NOT_RELATED_EXTERNAL_CLIENT)) {
            return UserInstance.createExternalUser(
                new ResourceOwner(new Username(USER_NAME), NON_CURATING_INSTITUTION), NON_CURATING_INSTITUTION);
        }

        var accessRights = getAccessRights(getRoles()).stream().toList();
        var userInstitution = switch (getUserInstitutionConfig()) {
            case BELONGS_TO_CREATING_INSTITUTION -> CREATING_INSTITUTION;
            case BELONGS_TO_CURATING_INSTITUTION -> CURATING_INSTITUTION;
            case BELONGS_TO_NON_CURATING_INSTITUTION -> NON_CURATING_INSTITUTION;
        };
        return UserInstance.create(USER_NAME, userInstitution, USER_CRISTIN_ID, accessRights, userInstitution);
    }

    private static HashSet<CuratingInstitution> createCuratingInstitutions() {
        var curatingInstitutions = new HashSet<CuratingInstitution>();
        curatingInstitutions.add(new CuratingInstitution(CURATING_INSTITUTION, Set.of()));
        return curatingInstitutions;
    }

    private static HashSet<AccessRight> getAccessRights(Set<PermissionsRole> roles) {
        return roleToAccessRightsMap.entrySet().stream()
                   .filter(entry -> roles.contains(entry.getKey()))
                   .map(Map.Entry::getValue)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toCollection(HashSet::new));
    }

    private Owner createOwner() {
        var userIsPublicationCreator = getRoles().contains(CREATOR)
                                       && BELONGS_TO_CREATING_INSTITUTION.equals(getUserInstitutionConfig())
                                       && nonNull(getUserInstance());
        return userIsPublicationCreator
                   ? new Owner(new User(getUserInstance().getUsername()), getUserInstance().getTopLevelOrgCristinId())
                   : new Owner(new User(randomString()), CREATING_INSTITUTION);
    }

    private Contributor createContributor() {
        var userIsContributor = getRoles().contains(CREATOR)
                                && BELONGS_TO_CURATING_INSTITUTION.equals(getUserInstitutionConfig())
                                && nonNull(getUserInstance());
        var identity = userIsContributor
                           ? new Identity.Builder()
                                 .withId(getUserInstance().getPersonCristinId())
                                 .withName(getUserInstance().getUsername())
                                 .build()
                           : new Identity.Builder().withId(randomUri()).withName(randomString()).build();
        return new Contributor.Builder()
                   .withAffiliations(List.of(Organization.fromUri(CURATING_INSTITUTION)))
                   .withIdentity(identity)
                   .withRole(new RoleType(Role.CREATOR))
                   .build();
    }

    private EntityDescription createEntityDescription(Resource resource) {
        return resource.getEntityDescription().copy()
                   .withContributors(List.of(createContributor()))
                   .build();
    }

    private Set<AdditionalIdentifierBase> addAdditionalIdentifiers(Resource resource) {
        var additionalIdentifiers = new HashSet<>(resource.getAdditionalIdentifiers());
        if (isImportedDegree()) {
            additionalIdentifiers.add(new AdditionalIdentifier(INSPERA, randomString()));
        }
        return additionalIdentifiers;
    }

    private AssociatedArtifactList createAssociatedArtifacts() {
        return switch (getFileConfig()) {
            case NO_FILES -> AssociatedArtifactList.empty();
            case NON_APPROVED_FILES_ONLY -> new AssociatedArtifactList(randomNonFinalizedFiles());
            case APPROVED_FILES -> new AssociatedArtifactList(randomFinalizedFiles());
        };
    }

    private List<PublicationChannel> generatePublicationChannelsForPublisher(Resource resource) {
        if (isNull(getUserInstance())) {
            return List.of();
        }

        if (ChannelClaimConfig.CLAIMED_BY_USERS_INSTITUTION.equals(getChannelClaimConfig())) {
            return List.of(
                createChannelClaimForPublisher(resource.getIdentifier(), getUserInstance().getTopLevelOrgCristinId()));
        }

        if (ChannelClaimConfig.CLAIMED_BY_NOT_USERS_INSTITUTION.equals(getChannelClaimConfig())) {
            var claimedBy = getUserInstance().getTopLevelOrgCristinId() != NON_CURATING_INSTITUTION
                                ? NON_CURATING_INSTITUTION
                                : CURATING_INSTITUTION;
            return List.of(createChannelClaimForPublisher(resource.getIdentifier(), claimedBy));
        }

        return List.of();
    }

    private PublicationChannel createChannelClaimForPublisher(SortableIdentifier resourceIdentifier,
                                                              URI organizationId) {
        return new ClaimedPublicationChannel(randomUri(),
                                             organizationId,
                                             organizationId,
                                             createConstraint(),
                                             ChannelType.PUBLISHER,
                                             SortableIdentifier.next(),
                                             resourceIdentifier,
                                             Instant.now(),
                                             Instant.now());
    }

    private Constraint createConstraint() {
        return new Constraint(getChannelClaimPublishingPolicy(),
                              getChannelClaimEditingPolicy(),
                              DEGREE_SCOPE);
    }
}
