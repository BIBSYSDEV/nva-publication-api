package no.unit.nva.expansion;

import static no.unit.nva.model.testing.PublicationGenerator.randomAdditionalIdentifier;
import static no.unit.nva.model.testing.PublicationGenerator.randomFundings;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomProjects;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.isNotNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.ConferenceAbstract;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceExpansionServiceNviCalculationTest extends ResourcesLocalTest {

    public static final int NUMBER_OF_DAYS_IN_YEAR = 365;
    public static final int FIRST_MONTH_IN_YEAR = 1;
    public static final int FIRST_DAY_OF_MONTH = 1;
    public static final String NON_NVI_CANDIDATE_JSON = "non_nvi_type_nvi_ candidate.json";
    public static final String NVI_CANDIDATE_JSON = "nvi_type_nvi_candidate.json";
    public static final String NVI_TYPE_FIELD_NAME = "nviType";
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private ResourceExpansionService expansionService;

    @BeforeEach
    void setUp() {
        super.init();
        initializeServices();
    }

    @Test
    void shouldIncludeNviTypeFieldForAllExpandedResources()
        throws JsonProcessingException, NotFoundException {

        var publication = randomPublication(AcademicArticle.class).copy()
                              .withEntityDescription(new EntityDescription())
                              .build();
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var framedResultNode = expandedResource.asJsonNode();
        var actualNviType = framedResultNode.at("/nviType");

        assertThat(actualNviType, isNotNull());
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNviCandidateWhenResourceMeetsAllNviCandidacyRequirements(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationMeetingAllNviCandidacyRequirements(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);

        var nviTypeObject = expandedResource.getAllFields().get(NVI_TYPE_FIELD_NAME);
        assertThat(nviTypeObject, isNotNull());

        var expectedNviTypeJsonString = stringFromResources(Path.of(NVI_CANDIDATE_JSON));
        assertThat(objectMapper.writeValueAsString(nviTypeObject), is(equalTo(expectedNviTypeJsonString)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceIsNotPublishedInCurrentYear(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationPublishedInYearBeforeCurrentYear(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var nviTypeObject = expandedResource.getAllFields().get(NVI_TYPE_FIELD_NAME);

        assertThat(nviTypeObject, isNotNull());

        var expectedNviTypeJsonString = stringFromResources(Path.of(NON_NVI_CANDIDATE_JSON));

        assertThat(objectMapper.writeValueAsString(nviTypeObject), is(equalTo(expectedNviTypeJsonString)));
    }

    @ParameterizedTest
    @MethodSource("nonNviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceDoesNotMeetPublicationInstanceAndPublicationContextRequirement(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = publicationWithEntityDescription(publicationInstance,
                                                           publicationContext,
                                                           PublicationStatus.PUBLISHED,
                                                           List.of(getVerifiedCreator()),
                                                           getRandomInstantInCurrentYear());
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var nviTypeObject = expandedResource.getAllFields().get(NVI_TYPE_FIELD_NAME);

        assertThat(nviTypeObject, isNotNull());

        var expectedNviTypeJsonString = stringFromResources(Path.of(NON_NVI_CANDIDATE_JSON));

        assertThat(objectMapper.writeValueAsString(nviTypeObject), is(equalTo(expectedNviTypeJsonString)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceIsNotPublished(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithStatusDraft(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var nviTypeObject = expandedResource.getAllFields().get(NVI_TYPE_FIELD_NAME);

        assertThat(nviTypeObject, isNotNull());

        var expectedNviTypeJsonString = stringFromResources(Path.of(NON_NVI_CANDIDATE_JSON));

        assertThat(objectMapper.writeValueAsString(nviTypeObject), is(equalTo(expectedNviTypeJsonString)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceContainsNonVerifiedContributorCreator(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithNonVerifiedCreator(publicationInstance, publicationContext);

        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var nviTypeObject = expandedResource.getAllFields().get(NVI_TYPE_FIELD_NAME);

        assertThat(nviTypeObject, isNotNull());

        var expectedNviTypeJsonString = stringFromResources(Path.of(NON_NVI_CANDIDATE_JSON));

        assertThat(objectMapper.writeValueAsString(nviTypeObject), is(equalTo(expectedNviTypeJsonString)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceDoesNotContainContributorCreator(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithoutCreator(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var nviTypeObject = expandedResource.getAllFields().get(NVI_TYPE_FIELD_NAME);

        assertThat(nviTypeObject, isNotNull());

        var expectedNviTypeJsonString = stringFromResources(Path.of(NON_NVI_CANDIDATE_JSON));

        assertThat(objectMapper.writeValueAsString(nviTypeObject), is(equalTo(expectedNviTypeJsonString)));
    }

    private static Stream<Arguments> nviCandidatePublicationInstanceAndPublicationContextProvider() {
        return Stream.of(Arguments.of(new AcademicArticle(null, randomString(), randomString(), randomString()),
                                      new Journal(randomUri())));
    }

    private static Stream<Arguments> nonNviCandidatePublicationInstanceAndPublicationContextProvider() {
        return Stream.of(Arguments.of(new ConferenceAbstract(randomString(), randomString(), randomString(), null),
                                      new Journal(randomUri())));
    }

    private static Reference getReferenceWithPublicationInstanceAndContext(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext) {
        var reference = new Reference();
        reference.setPublicationInstance(publicationInstance);
        reference.setPublicationContext(publicationContext);
        return reference;
    }

    private static EntityDescription getEntityDescriptionWithReferenceAndContributors(Reference reference,
                                                                                      List<Contributor> contributors) {
        var entityDescription = new EntityDescription();
        entityDescription.setReference(reference);
        entityDescription.setContributors(contributors);
        return entityDescription;
    }

    private static Instant getRandomInstantInCurrentYear() {
        var currentYear = LocalDate.now().getYear();
        return getRandomInstantInYear(currentYear);
    }

    private static Instant getRandomInstantBeforeCurrentYear() {
        var randomYearBeforeCurrentYear = LocalDate.now().minusYears((long) (Math.random() * 10)).getYear();
        return getRandomInstantInYear(randomYearBeforeCurrentYear);
    }

    private static Instant getRandomInstantInYear(int currentYear) {
        var startOfYear = LocalDate.of(currentYear, FIRST_MONTH_IN_YEAR, FIRST_DAY_OF_MONTH);
        var randomDays = (long) (Math.random() * NUMBER_OF_DAYS_IN_YEAR);
        var randomDateInCurrentYear = startOfYear.plusDays(randomDays);
        return randomDateInCurrentYear.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Publication getPublication(EntityDescription entityDescription, Instant dateInCurrentYear,
                                              PublicationStatus status) {
        return new Builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withStatus(status)
                   .withPublishedDate(dateInCurrentYear)
                   .withEntityDescription(entityDescription)
                   .withRightsHolder(randomString())
                   .withPublisher(randomOrganization())
                   .withSubjects(List.of(randomUri()))
                   .withModifiedDate(randomInstant())
                   .withAdditionalIdentifiers(Set.of(randomAdditionalIdentifier()))
                   .withProjects(randomProjects())
                   .withFundings(randomFundings())
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withLink(randomUri())
                   .withIndexedDate(randomInstant())
                   .withHandle(randomUri())
                   .withCreatedDate(randomInstant())
                   .withAssociatedArtifacts(AssociatedArtifactsGenerator.randomAssociatedArtifacts())
                   .build();
    }

    private Publication getPublicationPublishedInYearBeforeCurrentYear(
        PublicationInstance<? extends Pages> publicationInstance,
        PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance,
                                                publicationContext,
                                                PublicationStatus.PUBLISHED,
                                                List.of(getVerifiedCreator()),
                                                getRandomInstantBeforeCurrentYear());
    }

    private Publication getPublicationWithStatusDraft(PublicationInstance<? extends Pages> publicationInstance,
                                                      PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance,
                                                publicationContext,
                                                PublicationStatus.DRAFT,
                                                List.of(getVerifiedCreator()),
                                                getRandomInstantInCurrentYear());
    }

    private Publication getPublicationWithNonVerifiedCreator(PublicationInstance<? extends Pages> publicationInstance,
                                                             PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance,
                                                publicationContext,
                                                PublicationStatus.PUBLISHED,
                                                List.of(getNonVerifiedCreator()),
                                                getRandomInstantInCurrentYear());
    }

    private Publication getPublicationWithoutCreator(PublicationInstance<? extends Pages> publicationInstance,
                                                     PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance, publicationContext,
                                                PublicationStatus.PUBLISHED, List.of(
                createContributor(Role.EDITOR, ContributorVerificationStatus.VERIFIED)),
                                                getRandomInstantInCurrentYear());
    }

    private Publication getPublicationMeetingAllNviCandidacyRequirements(
        PublicationInstance<? extends Pages> publicationInstance,
        PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance,
                                                publicationContext,
                                                PublicationStatus.PUBLISHED,
                                                List.of(getVerifiedCreator()),
                                                getRandomInstantInCurrentYear());
    }

    private Publication publicationWithEntityDescription(
        PublicationInstance<? extends Pages> publicationInstance,
        PublicationContext publicationContext,
        PublicationStatus publicationStatus,
        List<Contributor> contributorList,
        Instant publishedDate) {

        var reference = getReferenceWithPublicationInstanceAndContext(publicationInstance, publicationContext);
        var entityDescription = getEntityDescriptionWithReferenceAndContributors(reference, contributorList);

        return getPublication(entityDescription, publishedDate, publicationStatus);
    }

    private Contributor getVerifiedCreator() {
        return createContributor(Role.CREATOR, ContributorVerificationStatus.VERIFIED);
    }

    private Contributor getNonVerifiedCreator() {
        return createContributor(Role.CREATOR, ContributorVerificationStatus.NOT_VERIFIED);
    }

    private Contributor createContributor(Role role, ContributorVerificationStatus verificationStatus) {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(role))
                   .withContributorVerificationStatus(verificationStatus)
                   .build();
    }

    private void initializeServices() {
        ResourceService resourceService = new ResourceService(client, CLOCK);
        UriRetriever uriRetriever = new UriRetriever();
        TicketService ticketService = new TicketService(client);
        expansionService = new ResourceExpansionServiceImpl(resourceService, ticketService, uriRetriever);
    }
}
