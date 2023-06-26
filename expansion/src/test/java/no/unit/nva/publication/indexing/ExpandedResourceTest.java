package no.unit.nva.publication.indexing;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleJournal;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSamplePublisher;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleSeries;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.utils.PublicationJsonPointers;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExpandedResourceTest {

    private static final String SERIES_LEVEL_JSON_PTR =
            "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
                    + "/series/level";
    private static final String PUBLISHER_LEVEL_JSON_PTR = "/entityDescription/reference/publicationContext"
            + "/entityDescription/reference/publicationContext"
            + "/publisher/level";
    private static final String PUBLISHER_ID_JSON_PTR =
            "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
                    + "/publisher/id";
    private static final String SERIES_ID_JSON_PTR =
            "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
                    + "/series/id";
    private static final String PUBLISHER_NAME_JSON_PTR =
            "/entityDescription/reference/publicationContext/publisher/name";
    private static final String SERIES_NAME_JSON_PTR =
            "/entityDescription/reference/publicationContext/series/name";
    private static final Set<String> ACCEPTABLE_FIELD_NAMES = Set.of("id", "name", "labels", "type", "hasPart");
    private static final String ID_NAMESPACE = System.getenv("ID_NAMESPACE");
    private static final URI HOST_URI = PublicationServiceConfig.PUBLICATION_HOST_URI;
    private UriRetriever uriRetriever;

    private static Set<URI> getOrgIdsForContributorAffiliations(Publication publication) {
        return publication
                .getEntityDescription()
                .getContributors()
                .stream()
                .map(Contributor::getAffiliations)
                .map(ExpandedResourceTest::getOrgIds)
                .flatMap(Set::stream).collect(Collectors.toSet());
    }

    private static Set<URI> getOrgIds(List<Organization> organizations) {
        return organizations.stream().map(Organization::getId).collect(Collectors.toSet());
    }

    private static void addPublisherToMockUriRetriever(UriRetriever uriRetriever, URI publisherId) throws IOException {
        var publicationChannelSamplePublisher = getPublicationChannelSamplePublisher(publisherId, randomString());
        mockGetRawContentResponse(uriRetriever, publisherId, publicationChannelSamplePublisher);
    }

    private static void addSeriesToMockUriRetriever(UriRetriever uriRetriever, URI seriesId) throws IOException {

        var publicationChannelSampleSeries = getPublicationChannelSampleSeries(seriesId, randomString());
        mockGetRawContentResponse(uriRetriever, seriesId, publicationChannelSampleSeries);
    }

    private static void mockGetRawContentResponse(UriRetriever uriRetriever, URI uri, String response) {
        when(uriRetriever.getRawContent(eq(uri), anyString()))
                .thenReturn(Optional.of(response));
    }

    private static List<URI> extractActualPublicationChannelUris(ObjectNode expandedResourceJsonNode) {
        var actualPublisherId = URI.create(expandedResourceJsonNode.at(PUBLISHER_ID_JSON_PTR).textValue());
        var actualSeriesId = URI.create(expandedResourceJsonNode.at(SERIES_ID_JSON_PTR).textValue());
        return List.of(actualPublisherId, actualSeriesId);
    }

    private static Publication getAcademicChapterPartOfAnthology(Publication bookAnthology) {
        var bookAnthologyUri = toPublicationId(bookAnthology.getIdentifier());
        var academicChapter = PublicationGenerator.randomPublication(AcademicChapter.class);
        var anthology = (Anthology) academicChapter.getEntityDescription().getReference().getPublicationContext();
        anthology.setId(bookAnthologyUri);
        return academicChapter;
    }

    private static Stream<String> getFieldNameStream(JsonNode topOrg) {
        var spliterator = Spliterators.spliteratorUnknownSize(topOrg.fieldNames(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    private static void assertFieldNameIsMemberOfAcceptableFieldNames(String fieldName) {
        assert ACCEPTABLE_FIELD_NAMES.contains(fieldName);
    }

    private static Contributor createContributorsWithEmptyAffiliations(Contributor contributor) {
        return new Contributor.Builder()
                .withIdentity(contributor.getIdentity())
                .withAffiliations(List.of(new Organization()))
                .withRole(contributor.getRole())
                .withSequence(contributor.getSequence())
                .withCorrespondingAuthor(contributor.isCorrespondingAuthor())
                .build();
    }

    private static void addPublicationChannelPublisherToMockUriRetriever(UriRetriever mockUriRetriever,
                                                                         URI journalId,
                                                                         String journalName,
                                                                         URI publisherId,
                                                                         String publisherName)
            throws IOException {
        var publicationChannelSampleJournal = getPublicationChannelSampleJournal(journalId, journalName);
        mockGetRawContentResponse(mockUriRetriever, journalId, publicationChannelSampleJournal);
        var publicationChannelSamplePublisher = getPublicationChannelSamplePublisher(publisherId, publisherName);
        mockGetRawContentResponse(mockUriRetriever, publisherId, publicationChannelSamplePublisher);
    }

    private static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    private static URI toPublicationId(SortableIdentifier identifier) {
        return UriWrapper.fromUri(ID_NAMESPACE)
                .addChild(identifier.toString())
                .getUri();
    }

    @BeforeEach
    void setup() {
        this.uriRetriever = mock(UriRetriever.class);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldReturnIndexDocumentWithValidReferenceData() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        final URI seriesUri = extractSeriesId(publication);
        final URI publisherUri = extractPublisherId(publication);
        final String publisherName = randomString();
        final String seriesName = randomString();

        final UriRetriever mockUriRetriever = mock(UriRetriever.class);
        addPublicationChannelPublisherToMockUriRetriever(mockUriRetriever, seriesUri, seriesName, publisherUri,
                publisherName);

        final ExpandedResource indexDocument = fromPublication(mockUriRetriever, publication);
        final JsonNode framedResultNode = indexDocument.asJsonNode();

        assertEquals(publisherUri.toString(),
                framedResultNode.at(PublicationJsonPointers.PUBLISHER_ID_JSON_PTR).textValue());
        assertEquals(publisherName, framedResultNode.at(PUBLISHER_NAME_JSON_PTR).textValue());
        assertEquals(seriesUri.toString(), framedResultNode.at(PublicationJsonPointers.SERIES_ID_JSON_PTR).textValue());
        assertEquals(seriesName, framedResultNode.at(SERIES_NAME_JSON_PTR).textValue());
    }

    //TODO: should check that you get affiliation names
    @ParameterizedTest(name = "should return IndexDocument with correct topLevelAffiliation with referencing depth: "
            + "{0}")
    @ValueSource(ints = {1, 2, 3, 6})
    void shouldReturnIndexDocumentWithCorrectTopLevelOrganization(int depth) throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        final URI seriesUri = extractSeriesId(publication);
        final URI publisherUri = extractPublisherId(publication);
        final URI affiliationToBeExpandedId = extractAffiliationsUris(publication).get(0);
        final String publisherName = randomString();
        final String seriesName = randomString();

        final UriRetriever mockUriRetriever = mock(UriRetriever.class);
        addPublicationChannelPublisherToMockUriRetriever(mockUriRetriever, seriesUri, seriesName, publisherUri,
                publisherName);

        var expectedTopLevelUri = getTopLevelUri(depth, affiliationToBeExpandedId, mockUriRetriever);

        ObjectNode framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();

        var expectedTopLevelOrgs = new ArrayList<URI>();
        expectedTopLevelOrgs.add(expectedTopLevelUri);
        expectedTopLevelOrgs.addAll(extractAffiliationsUris(publication).stream()
                .filter(aff -> !aff.equals(affiliationToBeExpandedId))
                .collect(Collectors.toList()));

        var distinctTopLevelIds = extractDistinctTopLevelIds(framedResultNode);

        assertThat(distinctTopLevelIds.stream().sorted().collect(Collectors.toList()),
                is(equalTo(expectedTopLevelOrgs.stream().sorted().collect(Collectors.toList()))));

        assertExplicitFieldsFromFraming(framedResultNode);
    }

    @Test
    void shouldNotCreateTopLevelOrgForBlankNodes() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        final URI seriesUri = extractSeriesId(publication);
        final URI publisherUri = extractPublisherId(publication);
        final String publisherName = randomString();
        final String seriesName = randomString();

        final UriRetriever mockUriRetriever = mock(UriRetriever.class);
        addPublicationChannelPublisherToMockUriRetriever(mockUriRetriever, seriesUri, seriesName, publisherUri,
                publisherName);

        publication.getEntityDescription().getContributors().get(0).getAffiliations().get(0).setId(null);

        ObjectNode framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();

        var affiliations = framedResultNode.findValues("affiliations").get(0);
        affiliations.forEach(
                aff -> {
                    if (!aff.has("id")) {
                        assertThat(aff.has("topLevelOrganization"), is(equalTo(false)));
                    }
                }
        );
    }

    @ParameterizedTest(name = "should return properly framed document with id based on Id-namespace and resource "
            + "identifier. Instance type:{0}")
    @MethodSource("publicationInstanceProvider")
    void shouldReturnDocumentWithIdBasedOnIdNameSpaceAndResourceIdentifier(Class<?> publicationInstance)
            throws JsonProcessingException {

        var publication = PublicationGenerator.randomPublication(publicationInstance);
        var indexDocument = fromPublication(uriRetriever, publication);
        var json = (ObjectNode) objectMapper.readTree(indexDocument.toJsonString());
        var expectedUri =
                UriWrapper.fromUri(HOST_URI).addChild(publication.getIdentifier().toString()).getUri();
        var actualUri = URI.create(json.at(PublicationJsonPointers.ID_JSON_PTR).textValue());
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnIndexDocumentContainingConfirmedSeriesUriFromNsdPublicationChannels()
            throws JsonProcessingException {

        Publication publication = randomBookWithConfirmedPublisher();
        Book book =
                extractBook(publication);
        Series series = (Series) book.getSeries();
        URI expectedSeriesUri = series.getId();
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        assertThat(actualDocument.getPublicationContextUris(), hasItems(expectedSeriesUri));
    }

    @Test
    void shouldReturnIndexDocumentContainingReturnsJournalUriFromNsdPublicationChannels()
            throws JsonProcessingException {

        Publication publication = randomJournalArticleWithConfirmedJournal();
        Journal journal =
                (Journal) publication.getEntityDescription().getReference().getPublicationContext();
        URI expectedJournalUri = journal.getId();
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        assertThat(actualDocument.getPublicationContextUris(), contains(expectedJournalUri));
    }

    @Test
    void shouldReturnIndexDocumentWithConfirmedSeriesIdWhenBookIsPartOfSeriesFoundInNsd()
            throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        Book book = extractBook(publication);
        Series confirmedSeries = (Series) book.getSeries();
        URI expectedSeriesId = confirmedSeries.getId();
        Publisher publisher = (Publisher) book.getPublisher();
        URI expectedPublisherId = publisher.getId();
        assertThat(actualDocument.getPublicationContextUris(),
                containsInAnyOrder(expectedSeriesId, expectedPublisherId));
    }

    @Test
    void shouldReturnIndexDocumentWithConfirmedJournalIdWhenPublicationIsPublishedInConfirmedJournal()
            throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(FeatureArticle.class);
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        Journal journal = extractJournal(publication);
        URI expectedJournalId = journal.getId();
        assertThat(actualDocument.getPublicationContextUris(), containsInAnyOrder(expectedJournalId));
    }

    @Test
    void shouldReturnExpandedResourceWithAnthologyPublicationChannelUrisWhenPublicationIsAcademicChapter()
            throws JsonProcessingException {
        var bookAnthology = PublicationGenerator.randomPublication(BookAnthology.class);
        var academicChapter = getAcademicChapterPartOfAnthology(bookAnthology);
        mockUriRetrieverPublicationResponse(bookAnthology);
        var expectedPublicationChannelIds = getPublicationContextUris(extractBook(bookAnthology));

        var expandedResource = fromPublication(uriRetriever, academicChapter);
        var expandedResourceJsonNode = expandedResource.asJsonNode();
        var actualPublicationChannelUris = extractActualPublicationChannelUris(expandedResourceJsonNode);
        assertThat(actualPublicationChannelUris,
                containsInAnyOrder(expectedPublicationChannelIds.toArray()));
    }

    @Test
    void shouldReturnExpandedResourceWithAnthologyPublicationChannelLevelWhenPublicationIsAcademicChapter()
            throws IOException {
        var bookAnthology = PublicationGenerator.randomPublication(BookAnthology.class);
        var academicChapter = getAcademicChapterPartOfAnthology(bookAnthology);
        mockUriRetrieverResponses(bookAnthology);

        var expandedResource = fromPublication(uriRetriever, academicChapter);
        var expandedResourceJsonNode = expandedResource.asJsonNode();
        var actualSeriesLevel = expandedResourceJsonNode.at(SERIES_LEVEL_JSON_PTR).textValue();
        var actualPublisherLevel = expandedResourceJsonNode.at(PUBLISHER_LEVEL_JSON_PTR).textValue();
        assertThat(actualSeriesLevel, is(not(nullValue())));
        assertThat(actualPublisherLevel, is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationContext() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().getReference().setPublicationContext(null);
        assertThat(ExpandedResource.fromPublication(uriRetriever, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationInstance() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().getReference().setPublicationInstance(null);
        assertThat(ExpandedResource.fromPublication(uriRetriever, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoMainTitle() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().setMainTitle(null);
        assertThat(ExpandedResource.fromPublication(uriRetriever, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenInputContainsAffiliationsThatAreIncomplete() {
        var publication = createPublicationWithEmptyAffiliations();
        assertDoesNotThrow(() -> ExpandedResource.fromPublication(uriRetriever, publication));
    }

    @Test
    void shouldUseApiVersionWhenLookingUpOrganizations() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var orgIds = getOrgIdsForContributorAffiliations(publication);
        ExpandedResource.fromPublication(uriRetriever, publication);
        orgIds.forEach(orgId -> verify(uriRetriever, times(1)).getRawContent(orgId, "application/ld+json; version=2023-05-26"));
    }

    private void mockUriRetrieverResponses(Publication bookAnthology) throws IOException {
        mockUriRetrieverPublicationChannelResponses(bookAnthology);
        mockUriRetrieverPublicationResponse(bookAnthology);
    }

    private void mockUriRetrieverPublicationChannelResponses(Publication bookAnthology) throws IOException {
        var seriesId = extractSeriesId(bookAnthology);
        var publisherId = extractPublisherId(bookAnthology);
        addSeriesToMockUriRetriever(uriRetriever, seriesId);
        addPublisherToMockUriRetriever(uriRetriever, publisherId);
    }

    private void mockUriRetrieverPublicationResponse(Publication publication) throws JsonProcessingException {
        var publicationId = toPublicationId(publication.getIdentifier());
        var publicationResponse = PublicationResponse.fromPublication(publication);
        publicationResponse.setId(publicationId);
        mockGetRawContentResponse(uriRetriever, publicationId, objectMapper.writeValueAsString(publicationResponse));
    }

    private List<URI> getPublicationContextUris(Book book) {
        var confirmedSeries = (Series) book.getSeries();
        var expectedSeriesId = confirmedSeries.getId();
        var publisher = (Publisher) book.getPublisher();
        var expectedPublisherId = publisher.getId();
        return List.of(expectedSeriesId, expectedPublisherId);
    }

    private void assertExplicitFieldsFromFraming(ObjectNode framedResultNode) {
        var node = framedResultNode.at("/topLevelOrganization");
        StreamSupport.stream(node.spliterator(), false)
                .flatMap(ExpandedResourceTest::getFieldNameStream)
                .forEach(ExpandedResourceTest::assertFieldNameIsMemberOfAcceptableFieldNames);
    }

    private URI getTopLevelUri(int depth, URI affiliationToBeExpandedId, UriRetriever mockUriRetriever) {
        var affiliationGenerator = new AffiliationGenerator(depth, mockUriRetriever);
        return affiliationGenerator.setAffiliationInMockUriRetriever(affiliationToBeExpandedId);
    }

    private List<URI> extractDistinctTopLevelIds(JsonNode framedResultNode) {
        var topLevelAffiliations = framedResultNode.at("/topLevelOrganization");
        return StreamSupport.stream(topLevelAffiliations.spliterator(), false)
                .map(node -> node.get("id").asText())
                .map(URI::create)
                .distinct()
                .collect(Collectors.toList());
    }

    private Publication createPublicationWithEmptyAffiliations() {
        var publication = PublicationGenerator.randomPublication();
        publication.setStatus(PUBLISHED);
        var entityDescription = publication.getEntityDescription();
        var contributors = entityDescription.getContributors().stream()
                .map(ExpandedResourceTest::createContributorsWithEmptyAffiliations)
                .collect(Collectors.toList());
        entityDescription.setContributors(contributors);
        return publication;
    }

    private Journal extractJournal(Publication publication) {
        return (Journal) publication.getEntityDescription().getReference().getPublicationContext();
    }

    private URI extractPublisherId(Publication publication) {
        Book book = extractBook(publication);
        Publisher publisher = extractPublisher(book);
        return publisher.getId();
    }

    private Publisher extractPublisher(Book book) {
        return (Publisher) book.getPublisher();
    }

    private List<URI> extractAffiliationsUris(Publication publication) {
        return publication.getEntityDescription().getContributors()
                .stream().flatMap(contributor ->
                        contributor.getAffiliations().stream().map(Organization::getId))
                .collect(Collectors.toList());
    }

    private URI extractSeriesId(Publication publication) {
        Book book = extractBook(publication);
        Series confirmedSeries = (Series) book.getSeries();
        return confirmedSeries.getId();
    }

    private Book extractBook(Publication publication) {
        return (Book) publication.getEntityDescription().getReference().getPublicationContext();
    }

    private Publication randomBookWithConfirmedPublisher() {
        return PublicationGenerator.randomPublication(BookMonograph.class);
    }

    private Publication randomJournalArticleWithConfirmedJournal() {
        return PublicationGenerator.randomPublication(FeatureArticle.class);
    }
}
