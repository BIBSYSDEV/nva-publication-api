package no.unit.nva.publication.indexing;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.PUBLISHER_ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.SERIES_ID_JSON_PTR;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_HOST_URI;
import static no.unit.nva.publication.indexing.AffiliationGenerator.getAffiliationsWithCommonParent;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleJournal;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSamplePublisher;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.utils.PublicationJsonPointers;
import no.unit.nva.expansion.utils.UriRetriever;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import nva.commons.core.paths.UriWrapper;
import org.hamcrest.CoreMatchers;
import org.hamcrest.text.IsEmptyString;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedResourceTest {

    public static final String PUBLISHER_NAME_JSON_PTR =
        "/entityDescription/reference/publicationContext/publisher/name";
    public static final String SERIES_NAME_JSON_PTR =
        "/entityDescription/reference/publicationContext/series/name";

    private UriRetriever uriRetriever;

    @BeforeEach
    void setup() {
        this.uriRetriever = mock(UriRetriever.class);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldReturnIndexDocumentWithValidReferenceData() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        final URI seriesUri = extractSeriesUri(publication);
        final URI publisherUri = extractPublisherUri(publication);
        final List<URI> affiliationUris = extractAffiliationsUris(publication);
        final String publisherName = randomString();
        final String seriesName = randomString();

        final UriRetriever mockUriRetriever = mock(UriRetriever.class);
        addPublicationChannelPublisherToMockUriRetriever(mockUriRetriever, seriesUri, seriesName, publisherUri,
                                                         publisherName);
        addAffiliationsWithParentNodeToMockUriRetriever(mockUriRetriever, affiliationUris);

        final ExpandedResource indexDocument = fromPublication(mockUriRetriever, publication);
        final JsonNode framedResultNode = indexDocument.asJsonNode();

        assertEquals(publisherUri.toString(), framedResultNode.at(PUBLISHER_ID_JSON_PTR).textValue());
        assertEquals(publisherName, framedResultNode.at(PUBLISHER_NAME_JSON_PTR).textValue());
        assertEquals(seriesUri.toString(), framedResultNode.at(SERIES_ID_JSON_PTR).textValue());
        assertEquals(seriesName, framedResultNode.at(SERIES_NAME_JSON_PTR).textValue());

        var hasPart = framedResultNode.findValue("https://example.org/vocab#hasPart");
        assertThat(hasPart.toString(), CoreMatchers.not(IsEmptyString.emptyOrNullString()));
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
            UriWrapper.fromUri(PUBLICATION_HOST_URI).addChild(publication.getIdentifier().toString()).getUri();
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

    private static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    private static void addPublicationChannelPublisherToMockUriRetriever(UriRetriever mockUriRetriever,
                                                                         URI journalId,
                                                                         String journalName,
                                                                         URI publisherId,
                                                                         String publisherName)
        throws IOException {
        String publicationChannelSampleJournal = getPublicationChannelSampleJournal(journalId, journalName);
        when(mockUriRetriever.getRawContent(eq(journalId), any()))
            .thenReturn(Optional.of(publicationChannelSampleJournal));
        String publicationChannelSamplePublisher = getPublicationChannelSamplePublisher(publisherId, publisherName);
        when(mockUriRetriever.getRawContent(eq(publisherId), any()))
            .thenReturn(Optional.of(publicationChannelSamplePublisher));
    }

    private void addAffiliationsWithParentNodeToMockUriRetriever(UriRetriever mockUriRetriever,
                                                                 List<URI> affiliationUris) {
        var parentURI = randomUri();

        Map<URI, String> uriToAffiliation = getAffiliationsWithCommonParent(affiliationUris, parentURI);

        uriToAffiliation.forEach((uri, content) ->
                                     when(mockUriRetriever.getRawContent(eq(uri), any()))
                                         .thenReturn(Optional.of(content))
        );
    }

    private Journal extractJournal(Publication publication) {
        return (Journal) publication.getEntityDescription().getReference().getPublicationContext();
    }

    private URI extractPublisherUri(Publication publication) {
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

    private URI extractSeriesUri(Publication publication) {
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
