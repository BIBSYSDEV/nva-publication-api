package no.unit.nva.publication.permissions;

import static java.util.UUID.randomUUID;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import nva.commons.apigateway.AccessRight;

public class PermissionsTestUtils {

    public static List<AccessRight> getAccessRightsForEditor() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_RESOURCES_ALL);
        accessRights.add(AccessRight.MANAGE_OWN_AFFILIATION);
        accessRights.add(AccessRight.MANAGE_CHANNEL_CLAIMS);
        return accessRights;
    }

    public static List<AccessRight> getAccessRightsForCurator() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_PUBLISHING_REQUESTS);
        accessRights.add(AccessRight.MANAGE_RESOURCES_STANDARD);
        accessRights.add(AccessRight.MANAGE_RESOURCE_FILES);
        return accessRights;
    }

    public static List<AccessRight> getAccessRightsForThesisCurator() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_DEGREE);
        accessRights.add(AccessRight.MANAGE_PUBLISHING_REQUESTS);
        accessRights.add(AccessRight.MANAGE_RESOURCES_STANDARD);
        accessRights.add(AccessRight.MANAGE_RESOURCE_FILES);
        return accessRights;
    }

    public static List<AccessRight> getAccessRightsForEmbargoThesisCurator() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_DEGREE);
        accessRights.add(AccessRight.MANAGE_DEGREE_EMBARGO);
        accessRights.add(AccessRight.MANAGE_PUBLISHING_REQUESTS);
        accessRights.add(AccessRight.MANAGE_RESOURCES_STANDARD);
        accessRights.add(AccessRight.MANAGE_RESOURCE_FILES);
        return accessRights;
    }

    public static List<AccessRight> getAccessRightsForRegistrator() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_OWN_RESOURCES);
        return accessRights;
    }

    public static List<AccessRight> getAccessRightsForContributor() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_OWN_RESOURCES);
        return accessRights;
    }

    public static void setPublicationChannel(Resource resource, Institution owner, ChannelPolicy publishingPolicy,
                                             ChannelPolicy editingPolicy) {
        var claimedChannel = new ClaimedPublicationChannel(randomUri(),
                                                           owner.getCustomerId(),
                                                           owner.getTopLevelCristinId(),
                                                           new Constraint(publishingPolicy, editingPolicy, List.of()),
                                                           PUBLISHER,
                                                           new SortableIdentifier(randomUUID().toString()),
                                                           resource.getIdentifier(),
                                                           Instant.now(), Instant.now());
        resource.setPublicationChannels(List.of(claimedChannel));
    }

    public static void setContributor(Publication publication, User contributor) {
        var contributors = List.of(createContributor(Role.CREATOR,
                                                     contributor.cristinId(),
                                                     contributor.topLevelCristinId()));
        publication.getEntityDescription().setContributors(contributors);
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(contributor.topLevelCristinId(), Set.of(contributor.cristinId()))));
    }

    private static Contributor createContributor(Role role, URI cristinPersonId, URI cristinOrganizationId) {
        return new Contributor.Builder()
                   .withAffiliations(List.of(Organization.fromUri(cristinOrganizationId)))
                   .withIdentity(new Identity.Builder().withId(cristinPersonId).withName(randomString()).build())
                   .withRole(new RoleType(role))
                   .build();
    }

    public record InstitutionSuite(Institution owningInstitution, Institution curatingInstitution,
                                   Institution nonCuratingInstitution) {

        public static InstitutionSuite random() {
            return new InstitutionSuite(Institution.random(), Institution.random(), Institution.random());
        }
    }

    public record Institution(User registrator, User contributor, User curator, User thesisCurator,
                              User embargoThesisCurator, User editor) {

        public static Institution random() {
            var customer = randomUri();
            var topLevelCristinId = randomUri();
            return new Institution(
                User.randomRegistrator(customer, topLevelCristinId),
                User.randomContributor(customer, topLevelCristinId),
                User.randomCurator(customer, topLevelCristinId),
                User.randomThesisCurator(customer, topLevelCristinId),
                User.randomEmbargoThesisCurator(customer, topLevelCristinId),
                User.randomEditor(customer, topLevelCristinId));
        }

        public URI getCustomerId() {
            return registrator.customer();
        }

        public URI getTopLevelCristinId() {
            return registrator.customer();
        }
    }

    public record User(String name, URI cristinId, URI customer, URI topLevelCristinId,
                       List<AccessRight> accessRights) {

        public static User random() {
            return new User(randomString(), randomUri(), randomUri(), randomUri(), List.of());
        }

        public static User randomRegistrator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForRegistrator());
        }

        public static User randomContributor(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForContributor());
        }

        public static User randomCurator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForCurator());
        }

        public static User randomThesisCurator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId,
                            getAccessRightsForThesisCurator());
        }

        public static User randomEmbargoThesisCurator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId,
                            getAccessRightsForEmbargoThesisCurator());
        }

        public static User randomEditor(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId,
                            getAccessRightsForEditor());
        }
    }
}
