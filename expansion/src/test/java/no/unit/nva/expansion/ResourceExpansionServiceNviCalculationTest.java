package no.unit.nva.expansion;

import static no.unit.nva.model.testing.PublicationGenerator.randomAdditionalIdentifier;
import static no.unit.nva.model.testing.PublicationGenerator.randomFundings;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomProjects;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSamplePublisher;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleSeries;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.ConferenceAbstract;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
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
    public static final String NVI_TYPE_JSON_POINTER = "/nviType";
    public static final String NVA_ONTOLOGY_NVI_CANDIDATE = "https://nva.sikt.no/ontology/publication#NviCandidate";
    public static final String NVI_TYPE_ID_FIELD_NAME = "id";
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private static final String NVA_ONTOLOGY_NON_NVI_CANDIDATE = "https://nva.sikt"
                                                                 + ".no/ontology/publication#NviCandidate";
    private ResourceExpansionService expansionService;

    private UriRetriever uriRetriever;

    @BeforeEach
    void setUp() {
        super.init();
        var resourceService = new ResourceService(client, CLOCK);
        uriRetriever = mock(UriRetriever.class);
        var ticketService = new TicketService(client);
        expansionService = new ResourceExpansionServiceImpl(resourceService, ticketService, uriRetriever);
    }

    @Test
    void shouldIncludeNviTypeFieldForAllExpandedResources() throws IOException, NotFoundException {
        var publication = randomPublication();
        mockUriRetriever(uriRetriever);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER);

        assertThat(actualNviType, is(notNullValue()));
    }

    @Test
    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNviCandidateWhenResourceMeetsAllNviCandidacyRequirements(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationMeetingAllNviCandidacyRequirements(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = objectMapper.convertValue(expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER),
                                                      new TypeReference<Map<String, Object>>() {
                                                      }).get(NVI_TYPE_ID_FIELD_NAME);

        assertThat(actualNviType, is(equalTo(NVA_ONTOLOGY_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceIsNotPublishedInCurrentYear(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationPublishedInYearBeforeCurrentYear(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = objectMapper.convertValue(expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER),
                                                      new TypeReference<Map<String, Object>>() {
                                                      }).get(NVI_TYPE_ID_FIELD_NAME);

        assertThat(actualNviType, is(equalTo(NVA_ONTOLOGY_NON_NVI_CANDIDATE)));
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
        var actualNviType = objectMapper.convertValue(expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER),
                                                      new TypeReference<Map<String, Object>>() {
                                                      }).get(NVI_TYPE_ID_FIELD_NAME);

        assertThat(actualNviType, is(equalTo(NVA_ONTOLOGY_NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceIsNotPublished(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithStatusDraft(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = objectMapper.convertValue(expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER),
                                                      new TypeReference<Map<String, Object>>() {
                                                      }).get(NVI_TYPE_ID_FIELD_NAME);

        assertThat(actualNviType, is(equalTo(NVA_ONTOLOGY_NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceContainsNonVerifiedContributorCreator(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithNonVerifiedCreator(publicationInstance, publicationContext);

        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = objectMapper.convertValue(expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER),
                                                      new TypeReference<Map<String, Object>>() {
                                                      }).get(NVI_TYPE_ID_FIELD_NAME);

        assertThat(actualNviType, is(equalTo(NVA_ONTOLOGY_NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceDoesNotContainContributorCreator(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithoutCreator(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = objectMapper.convertValue(expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER),
                                                      new TypeReference<Map<String, Object>>() {
                                                      }).get(NVI_TYPE_ID_FIELD_NAME);

        assertThat(actualNviType, is(equalTo(NVA_ONTOLOGY_NON_NVI_CANDIDATE)));
    }

    private static URI randomPublicationChannelsUri() {
        return URI.create("https://api.dev.nva.aws.unit.no/publication-channels/" + randomString());
    }

    private static void addPublicationChannelPublisherToMockUriRetriever(UriRetriever mockUriRetriever,
                                                                         URI seriesId,
                                                                         String seriesName,
                                                                         URI publisherId,
                                                                         String publisherName)
        throws IOException {
        String publicationChannelSampleJournal = getPublicationChannelSampleSeries(seriesId, seriesName);
        when(mockUriRetriever.getRawContent(eq(seriesId), any()))
            .thenReturn(Optional.of(publicationChannelSampleJournal));
        String publicationChannelSamplePublisher = getPublicationChannelSamplePublisher(publisherId, publisherName);
        when(mockUriRetriever.getRawContent(eq(publisherId), any()))
            .thenReturn(Optional.of(publicationChannelSamplePublisher));
    }

    private static void mockUriRetriever(UriRetriever mockUriRetriever)
        throws IOException {
        when(mockUriRetriever.getRawContent(any(), any()))
            .thenReturn(Optional.empty());
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

    private Publication randomBookWithConfirmedPublisher() {
        return PublicationGenerator.randomPublication(BookMonograph.class);
    }

    private URI extractSeriesUri(Publication publication) {
        Book book = extractBook(publication);
        Series confirmedSeries = (Series) book.getSeries();
        return confirmedSeries.getId();
    }

    private Book extractBook(Publication publication) {
        return (Book) publication.getEntityDescription().getReference().getPublicationContext();
    }

    private URI extractPublisherUri(Publication publication) {
        Book book = extractBook(publication);
        Publisher publisher = extractPublisher(book);
        return publisher.getId();
    }

    private List<URI> extractAffiliationsUris(Publication publication) {
        return publication.getEntityDescription().getContributors()
                   .stream().flatMap(contributor ->
                                         contributor.getAffiliations().stream().map(Organization::getId))
                   .collect(Collectors.toList());
    }

    private Publisher extractPublisher(Book book) {
        return (Publisher) book.getPublisher();
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
}
