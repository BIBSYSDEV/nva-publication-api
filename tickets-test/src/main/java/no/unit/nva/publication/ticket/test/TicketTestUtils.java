package no.unit.nva.publication.ticket.test;

import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.testing.PublicationGenerator.fromInstanceClassesExcluding;
import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomEntityDescription;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.License;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.params.provider.Arguments;

public final class TicketTestUtils {

    private static final Set<PublicationStatus> PUBLISHED_STATUSES = Set.of(PUBLISHED, PUBLISHED_METADATA);
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final URI CURATING_INSTITUTION_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

    private TicketTestUtils() {
    }

    public static Stream<Arguments> ticketTypeAndPublicationStatusProvider() {
        return Stream.of(Arguments.of(DoiRequest.class, PUBLISHED),
                         Arguments.of(DoiRequest.class, PUBLISHED_METADATA),
                         Arguments.of(PublishingRequestCase.class, DRAFT),
                         Arguments.of(GeneralSupportRequest.class, DRAFT));
    }

    public static Stream<Arguments> ticketTypeAndAccessRightProvider() {
        return Stream.of(Arguments.of(PUBLISHED, DoiRequest.class, AccessRight.MANAGE_DOI),
                         Arguments.of(DRAFT, PublishingRequestCase.class, AccessRight.MANAGE_PUBLISHING_REQUESTS),
                         Arguments.of(DRAFT, GeneralSupportRequest.class, AccessRight.SUPPORT));
    }

    public static Stream<Arguments> invalidAccessRightForTicketTypeProvider() {
        return Stream.of(Arguments.of(DoiRequest.class, AccessRight.MANAGE_PUBLISHING_REQUESTS),
                         Arguments.of(PublishingRequestCase.class, AccessRight.MANAGE_DOI),
                         Arguments.of(GeneralSupportRequest.class, AccessRight.MANAGE_PUBLISHING_REQUESTS));
    }

    public static Publication createNonPersistedPublication(PublicationStatus status) {
        return randomPublicationWithStatus(status);
    }

    public static Publication createPersistedNonDegreePublication(URI publisherId, PublicationStatus status,
                                                                  ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomNonDegreePublication(status)
                              .copy()
                              .withPublisher(new Organization.Builder()
                                                 .withId(publisherId)
                                                 .build())
                              .build();
        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublication(URI publisherId, PublicationStatus status,
                                                         ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomNonDegreePublication(status);
        if (publisherId != null) {
            publication = publication.copy()
                              .withPublisher(new Organization.Builder().withId(publisherId).build())
                              .build();
        }
        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublication(PublicationStatus status, ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomNonDegreePublication(status);
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear("2020").build());
        publication.getEntityDescription().getContributors().forEach(contributor ->
                                                                         contributor.getAffiliations()
                                                                             .forEach(TicketTestUtils::setAffiliation)
        );
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(CURATING_INSTITUTION_ID, getContributorIds(publication))));
        return persistPublication(resourceService, publication);
    }

    private static Set<URI> getContributorIds(Publication publication) {
        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .collect(Collectors.toSet());
    }

    public static Publication createPersistedDegreePublication(PublicationStatus status,
                                                               ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublication(DegreePhd.class);
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear("2020").build());
        publication.getEntityDescription().getContributors().forEach(contributor ->
                                                                         contributor.getAffiliations()
                                                                             .forEach(TicketTestUtils::setAffiliation)
        );
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(CURATING_INSTITUTION_ID, getContributorIds(publication))));
        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublishedPublicationWithUnpublishedFilesAndContributor(
        URI userCristinId,
        ResourceService resourceService) throws ApiGatewayException {
        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(new AssociatedArtifactList())
                              .build();

        var identity = new Identity.Builder().withName(randomString()).withId(userCristinId).build();
        var contributor = new Contributor.Builder().withIdentity(identity).withRole(new RoleType(Role.CREATOR)).build();
        var entityDesc = publication.getEntityDescription().copy().withContributors(List.of(contributor)).build();
        var publicationWithContributor = publication.copy().withEntityDescription(entityDesc).build();

        return persistPublication(resourceService, publicationWithContributor);
    }

    public static Publication createPersistedPublishedPublicationWithUnpublishedFilesAndOwner(
        String owner,
        ResourceService resourceService) throws ApiGatewayException {
        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withStatus(PUBLISHED)
                              .withResourceOwner(new ResourceOwner(new Username(owner), randomUri()))
                              .withAssociatedArtifacts(new AssociatedArtifactList())
                              .build();
        var publicationWithContributor = publication.copy().build();

        return persistPublication(resourceService, publicationWithContributor);
    }

    private static Publication persistPublication(ResourceService resourceService, Publication publication)
        throws ApiGatewayException {
        var persistedPublication = Resource
                                       .fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));
        if (isPublished(publication)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static Publication createPersistedPublicationWithAdministrativeAgreement(ResourceService resourceService)
        throws ApiGatewayException {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES).copy()
                              .withAssociatedArtifacts(List.of(administrativeAgreement()))
                              .build();

        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublicationWithAdministrativeAgreement(URI publisherId,
                                                                                    ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomNonDegreePublication(
            randomElement(Arrays.stream(PublicationStatus.values()).toList())).copy()
                              .withAssociatedArtifacts(List.of(administrativeAgreement()))
                              .withPublisher(new Builder()
                                                 .withId(publisherId)
                                                 .build())
                              .build();

        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublicationWithUnpublishedFiles(PublicationStatus status,
                                                                             ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithUnpublishedFiles(status);
        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublicationWithUnpublishedFiles(URI publisher,
                                                                             PublicationStatus status,
                                                                             ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithUnpublishedFiles(publisher, status);
        return persistPublication(resourceService, publication);
    }

    public static Publication createdPersistedPublicationWithoutMainTitle(PublicationStatus status,
                                                                          ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithUnpublishedFiles(status);
        publication.getEntityDescription().setMainTitle(null);
        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublicationWithPublishedFiles(URI customerId, PublicationStatus status,
                                                                           ResourceService resourceService)
        throws ApiGatewayException {
        var publisher = new Builder().withId(customerId).build();
        var publication = randomPublicationWithPublishedFiles(status).copy().withPublisher(publisher).build();
        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublicationWithAssociatedLink(PublicationStatus status,
                                                                           ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithAssociatedLink(status);
        return persistPublication(resourceService, publication);
    }

    public static Publication createPersistedPublicationWithDoi(PublicationStatus status,
                                                                ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithStatusAndDoi(status);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                                    UserInstance.fromPublication(
                                                                                        publication));
        if (PUBLISHED.equals(status)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static Publication createPersistedPublicationWithOwner(PublicationStatus status,
                                                                  UserInstance owner,
                                                                  ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithStatusAndOwner(status, owner);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, owner);
        if (PUBLISHED.equals(status) || PUBLISHED_METADATA.equals(status)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static TicketEntry createPersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType,
                                                    TicketService ticketService)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType)
                   .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation())
                   .withOwner(UserInstance.fromPublication(publication).getUsername())
                   .persistNewTicket(ticketService);
    }

    public static TicketEntry createClosedTicket(Publication publication, Class<? extends TicketEntry> ticketType,
                                                 TicketService ticketService)
        throws ApiGatewayException {
        return TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                   .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation())
                   .persistNewTicket(ticketService).close(new Username("Username"));
    }

    public static TicketEntry createCompletedTicket(Publication publication, Class<? extends TicketEntry> ticketType,
                                                    TicketService ticketService)
        throws ApiGatewayException {
        var completedTicket = TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                                  .withOwner(UserInstance.fromPublication(publication).getUsername())
                                  .persistNewTicket(ticketService).complete(publication, new Username("Username"));
        completedTicket.persistUpdate(ticketService);
        return completedTicket;
    }

    public static TicketEntry createNonPersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ConflictException {
        return TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                   .withOwner(UserInstance.fromPublication(publication).getUsername());
    }

    private static void setAffiliation(Corporation affiliation) {
        ((Organization) affiliation).setId(
            URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"));
    }

    private static Publication randomPublicationWithPublishedFiles(PublicationStatus status) {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES).copy()
                              .withStatus(status)
                              .build();
        publishFiles(publication);
        return publication;
    }

    private static boolean isPublished(Publication publication) {
        return PUBLISHED_STATUSES.contains(publication.getStatus());
    }

    private static void publishPublication(ResourceService resourceService, Publication persistedPublication)
        throws ApiGatewayException {
        resourceService.publishPublication(UserInstance.fromPublication(persistedPublication),
                                           persistedPublication.getIdentifier());
    }

    private static Publication randomPublicationWithStatusAndOwner(PublicationStatus status, UserInstance owner) {
        return randomPublicationWithStatus(status).copy()
                   .withResourceOwner(
                       new ResourceOwner(new Username(owner.getUsername()), owner.getTopLevelOrgCristinId()))
                   .build();
    }

    private static Publication randomPublicationWithStatus(PublicationStatus status) {
        return fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES).copy()
                   .withDoi(null)
                   .withStatus(status)
                   .build();
    }

    private static Publication randomPublicationWithUnpublishedFiles(PublicationStatus status) {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES).copy()
                              .withStatus(status)
                              .build();
        unpublishFiles(publication);
        return publication;
    }

    private static Publication randomPublicationWithUnpublishedFiles(URI publisherId, PublicationStatus status) {
        var publication = randomNonDegreePublication(PUBLISHED).copy()
                              .withPublisher(new Organization.Builder()
                                                 .withId(publisherId)
                                                 .build())
                              .withStatus(status)
                              .build();
        unpublishFiles(publication);
        return publication;
    }

    private static Publication randomPublicationWithAssociatedLink(PublicationStatus status) {
        return fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES).copy()
                   .withStatus(status)
                   .withAssociatedArtifacts(List.of(new AssociatedLink(randomUri(), null, null)))
                   .build();
    }

    private static Publication randomNonDegreePublication(PublicationStatus status) {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES);
        return publication.copy()
                   .withStatus(status)
                   .withDoi(null)
                   .build();
    }

    private static <T> T randomElement(List<T> elements) {
        return elements.get(RANDOM.nextInt(elements.size()));
    }

    private static void unpublishFiles(Publication publication) {
        var list = publication.getAssociatedArtifacts()
                       .stream()
                       .filter(File.class::isInstance)
                       .map(File.class::cast)
                       .map(File::toUnpublishedFile)
                       .collect(Collectors.toCollection(() -> new ArrayList<AssociatedArtifact>()));
        publication.setAssociatedArtifacts(new AssociatedArtifactList(list));
    }

    private static void publishFiles(Publication publication) {
        var list = publication.getAssociatedArtifacts()
                       .stream()
                       .filter(File.class::isInstance)
                       .map(File.class::cast)
                       .map(File::toPublishedFile)
                       .collect(Collectors.toCollection(() -> new ArrayList<AssociatedArtifact>()));
        publication.setAssociatedArtifacts(new AssociatedArtifactList(list));
    }

    private static Publication randomPublicationWithStatusAndDoi(PublicationStatus status) {
        return PublicationGenerator.randomPublication().copy()
                   .withDoi(randomDoi())
                   .withStatus(status)
                   .build();
    }

    private static AdministrativeAgreement administrativeAgreement() {
        var license = new License.Builder().withLink(randomUri()).withIdentifier("identifier").build();
        return new AdministrativeAgreement(UUID.randomUUID(), "name", "application/json",
                                           123124124L, license, true, null, null, new UserUploadDetails(null, null));
    }
}
