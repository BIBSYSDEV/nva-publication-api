package no.unit.nva.expansion;

import static no.unit.nva.model.testing.PublicationGenerator.randomAdditionalIdentifier;
import static no.unit.nva.model.testing.PublicationGenerator.randomFundings;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomProjects;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelJournal;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSamplePublisher;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleSeries;
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
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
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

    private static final String NVI_TYPE_JSON_POINTER = "/nviType";
    private static final String NVI_CANDIDATE = "NviCandidate";
    private static final String NON_NVI_CANDIDATE = "NonNviCandidate";
    private static final String PUBLICATION_CHANNELS_BASE_URI = "https://api.dev.nva.aws.unit.no/publication-channels/";
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private static final int CURRENT_NVI_YEAR = 2023;
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
        //TODO: fix setup level
    void shouldSetNviTypeNviCandidateWhenAcademicChapterMeetsAllNviCandidacyRequirements()
        throws IOException, NotFoundException {

        Publication publication = setupAndMockAcademicChapterMeetingAllNviCandidacyRequirements();
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NVI_CANDIDATE)));
    }

    @Test
    void shouldSetNviTypeNviCandidateWhenAcademicMonographMeetsAllNviCandidacyRequirements()
        throws IOException, NotFoundException, InvalidIsbnException {

        var publication = setupAndMockAcademicMonographMeetingAllNviCandidacyRequirements();
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidateJournalProvider")
    void shouldSetNviTypeNviCandidateWhenPublicationInJournalMeetsAllNviCandidacyRequirements(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws IOException, NotFoundException {

        var publication = setupAndMockPublicationInJournalMeetingAllNviCandidacyRequirements(publicationInstance,
                                                                                             publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidateJournalProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourcePublishedBeforeCurrentYear(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublishedPublicationWithVerifiedCreator(publicationInstance, publicationContext,
                                                                     getPublicationDateBeforeCurrentNviYear());
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidateJournalProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourcePublishedAfterCurrentYear(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublishedPublicationWithVerifiedCreator(publicationInstance, publicationContext,
                                                                     getPublicationDateAfterCurrentNviYear());
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nonNviCandidatePublicationInstanceAndPublicationContextProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceDoesNotMeetPublicationInstanceAndPublicationContextRequirement(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublishedPublicationWithVerifiedCreator(publicationInstance, publicationContext,
                                                                     getCurrentNviPublicationDate());
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidateJournalProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceIsNotPublished(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithStatusDraft(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidateJournalProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceContainsNonVerifiedContributorCreator(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithNonVerifiedCreator(publicationInstance, publicationContext);

        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NON_NVI_CANDIDATE)));
    }

    @ParameterizedTest
    @MethodSource("nviCandidateJournalProvider")
    void shouldSetNviTypeNonNviCandidateWhenResourceDoesNotContainContributorCreator(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext)
        throws JsonProcessingException, NotFoundException {

        var publication = getPublicationWithoutCreator(publicationInstance, publicationContext);
        var resourceUpdate = Resource.fromPublication(publication);

        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualNviType = expandedResource.asJsonNode().at(NVI_TYPE_JSON_POINTER).asText();

        assertThat(actualNviType, is(equalTo(NON_NVI_CANDIDATE)));
    }

    private static URI extractAnthologyUri(Publication publication) {
        var test = (Anthology) publication.getEntityDescription().getReference().getPublicationContext();
        return test.getId();
    }

    private static URI randomPublicationChannelsUri() {
        return URI.create(PUBLICATION_CHANNELS_BASE_URI + randomString());
    }

    private static void addPublisherToMockUriRetriever(UriRetriever mockUriRetriever,
                                                       URI publisherId,
                                                       String publisherName)
        throws IOException {
        var publicationChannelSamplePublisher = getPublicationChannelSamplePublisher(publisherId, publisherName);
        when(mockUriRetriever.getRawContent(eq(publisherId), any()))
            .thenReturn(Optional.of(publicationChannelSamplePublisher));
    }

    private static void addJournalToMockUriRetriever(UriRetriever mockUriRetriever,
                                                     URI journalId,
                                                     String journalName)
        throws IOException {

        var publicationChannelSampleJournal = getPublicationChannelJournal(journalId, journalName);
        when(mockUriRetriever.getRawContent(eq(journalId), any()))
            .thenReturn(Optional.of(publicationChannelSampleJournal));
    }

    private static void addSeriesToMockUriRetriever(UriRetriever mockUriRetriever,
                                                    URI seriesId,
                                                    String seriesName)
        throws IOException {

        var publicationChannelSampleSeries = getPublicationChannelSampleSeries(seriesId, seriesName);
        when(mockUriRetriever.getRawContent(eq(seriesId), any()))
            .thenReturn(Optional.of(publicationChannelSampleSeries));
    }

    private static void mockUriRetriever(UriRetriever mockUriRetriever) {
        when(mockUriRetriever.getRawContent(any(), any()))
            .thenReturn(Optional.empty());
    }

    private static Stream<Arguments> nviCandidateJournalProvider() {
        return Stream.of(Arguments.of(new AcademicArticle(null, randomString(), randomString(), randomString()),
                                      new Journal(randomPublicationChannelsUri())),
                         Arguments.of(new AcademicLiteratureReview(null, randomString(), randomString(),
                                                                   randomString()),
                                      new Journal(randomPublicationChannelsUri()))
        );
    }

    private static Stream<Arguments> nonNviCandidatePublicationInstanceAndPublicationContextProvider() {
        return Stream.of(Arguments.of(new ConferenceAbstract(randomString(), randomString(), randomString(), null),
                                      new Journal(randomPublicationChannelsUri())));
    }

    private static Reference getReferenceWithPublicationInstanceAndContext(
        PublicationInstance<? extends Pages> publicationInstance, PublicationContext publicationContext) {
        var reference = new Reference();
        reference.setPublicationInstance(publicationInstance);
        reference.setPublicationContext(publicationContext);
        return reference;
    }

    private static EntityDescription getEntityDescriptionWithReferenceAndContributors(Reference reference,
                                                                                      List<Contributor> contributors,
                                                                                      PublicationDate publicationDate) {
        var entityDescription = new EntityDescription();
        entityDescription.setReference(reference);
        entityDescription.setContributors(contributors);
        entityDescription.setPublicationDate(publicationDate);
        return entityDescription;
    }

    private static PublicationDate getCurrentNviPublicationDate() {
        return new PublicationDate.Builder().withYear(String.valueOf(CURRENT_NVI_YEAR)).build();
    }

    private static PublicationDate getPublicationDateBeforeCurrentNviYear() {
        var randomYearBeforeCurrentYear = LocalDate.of(CURRENT_NVI_YEAR, 1, 1)
                                              .minusYears((long) (Math.random() * 10))
                                              .getYear();
        return new PublicationDate.Builder().withYear(String.valueOf(randomYearBeforeCurrentYear)).build();
    }

    private static PublicationDate getPublicationDateAfterCurrentNviYear() {
        var randomYearAfterCurrentYear =
            LocalDate.of(CURRENT_NVI_YEAR, 1, 1).plusYears((long) (Math.random() * 10)).getYear();
        return new PublicationDate.Builder().withYear(String.valueOf(randomYearAfterCurrentYear)).build();
    }

    private static Publication getPublication(EntityDescription entityDescription,
                                              PublicationStatus status) {
        return new Builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withStatus(status)
                   .withPublishedDate(randomInstant())
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

    private Publication setupAndMockAcademicChapterMeetingAllNviCandidacyRequirements() throws IOException {
        var publication = getPublicationMeetingAllNviCandidacyRequirements(new AcademicChapter(null),
                                                                           new Anthology.Builder()
                                                                               .withId(randomPublicationChannelsUri())
                                                                               .build());
        var publisherUri = extractAnthologyUri(publication);
        var publisherName = randomString();

        addPublisherToMockUriRetriever(uriRetriever, publisherUri, publisherName);
        return publication;
    }

    private Publication setupAndMockAcademicMonographMeetingAllNviCandidacyRequirements()
        throws InvalidIsbnException, IOException {
        var publication = getPublicationMeetingAllNviCandidacyRequirements(new AcademicMonograph(null),
                                                                           new Book.BookBuilder()
                                                                               .withSeries(new Series(
                                                                                   randomPublicationChannelsUri()))
                                                                               .withPublisher(new Publisher(
                                                                                   randomPublicationChannelsUri()))
                                                                               .build());
        var seriesUri = extractSeriesUri(publication);
        var publisherUri = extractPublisherUri(publication);
        var publisherName = randomString();
        var seriesName = randomString();

        addSeriesToMockUriRetriever(uriRetriever, seriesUri, seriesName);
        addPublisherToMockUriRetriever(uriRetriever, publisherUri, publisherName);
        return publication;
    }

    private Publication setupAndMockPublicationInJournalMeetingAllNviCandidacyRequirements(
        PublicationInstance<? extends Pages> publicationInstance,
        PublicationContext publicationContext) throws IOException {
        var publication = getPublicationMeetingAllNviCandidacyRequirements(publicationInstance, publicationContext);
        var journalUri = extractJournalUri(publication);
        var journalName = randomString();

        addJournalToMockUriRetriever(uriRetriever, journalUri, journalName);
        return publication;
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

    private URI extractJournalUri(Publication publication) {
        var journal = (Journal) publication.getEntityDescription().getReference().getPublicationContext();
        return journal.getId();
    }

    private Publisher extractPublisher(Book book) {
        return (Publisher) book.getPublisher();
    }

    private Publication getPublishedPublicationWithVerifiedCreator(
        PublicationInstance<? extends Pages> publicationInstance,
        PublicationContext publicationContext, PublicationDate publicationDate) {
        return publicationWithEntityDescription(publicationInstance,
                                                publicationContext,
                                                PublicationStatus.PUBLISHED,
                                                List.of(getVerifiedCreator()),
                                                publicationDate);
    }

    private Publication getPublicationWithStatusDraft(PublicationInstance<? extends Pages> publicationInstance,
                                                      PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance,
                                                publicationContext,
                                                PublicationStatus.DRAFT,
                                                List.of(getVerifiedCreator()),
                                                getCurrentNviPublicationDate());
    }

    private Publication getPublicationWithNonVerifiedCreator(PublicationInstance<? extends Pages> publicationInstance,
                                                             PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance,
                                                publicationContext,
                                                PublicationStatus.PUBLISHED,
                                                List.of(getNonVerifiedCreator()),
                                                getCurrentNviPublicationDate());
    }

    private Publication getPublicationWithoutCreator(PublicationInstance<? extends Pages> publicationInstance,
                                                     PublicationContext publicationContext) {
        return publicationWithEntityDescription(publicationInstance, publicationContext,
                                                PublicationStatus.PUBLISHED, List.of(
                createContributor(Role.EDITOR, ContributorVerificationStatus.VERIFIED)),
                                                getCurrentNviPublicationDate());
    }

    private Publication getPublicationMeetingAllNviCandidacyRequirements(
        PublicationInstance<? extends Pages> publicationInstance,
        PublicationContext publicationContext) {
        return getPublishedPublicationWithVerifiedCreator(publicationInstance, publicationContext,
                                                          getCurrentNviPublicationDate());
    }

    private Publication publicationWithEntityDescription(
        PublicationInstance<? extends Pages> publicationInstance,
        PublicationContext publicationContext,
        PublicationStatus publicationStatus,
        List<Contributor> contributorList,
        PublicationDate publicationDate) {

        var reference = getReferenceWithPublicationInstanceAndContext(publicationInstance, publicationContext);
        var entityDescription = getEntityDescriptionWithReferenceAndContributors(reference, contributorList,
                                                                                 publicationDate);

        return getPublication(entityDescription, publicationStatus);
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
