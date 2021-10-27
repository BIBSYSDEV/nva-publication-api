package no.unit.nva.publication.indexing;

import static no.unit.nva.publication.indexing.IndexDocument.ID_NAMESPACE;
import static no.unit.nva.publication.indexing.IndexDocument.fromPublication;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleJournal;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSamplePublisher;
import static no.unit.nva.publication.indexing.PublicationJsonPointers.PUBLISHER_ID_JSON_PTR;
import static no.unit.nva.publication.indexing.PublicationJsonPointers.SERIES_ID_JSON_PTR;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.publication.PublicationGenerator;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class IndexDocumentTest {

    public static final String PUBLISHER_NAME_JSON_PTR =
        "/entityDescription/reference/publicationContext/publisher/name";
    public static final String SERIES_NAME_JSON_PTR =
        "/entityDescription/reference/publicationContext/series/name";

    @Test
    public void shouldReturnIndexDocumentWithValidReferenceData() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        final URI seriesUri = extractSeriesUri(publication);
        final URI publisherUri = extractPublisherUri(publication);
        final String publisherName = randomString();
        final String seriesName = randomString();

        final UriRetriever mockUriRetriever =
            mockPublicationChannelPublisherResponse(seriesUri, seriesName, publisherUri, publisherName);
        final IndexDocument indexDocument = fromPublication(mockUriRetriever, publication);
        final JsonNode framedResultNode = indexDocument.asJsonNode();

        assertEquals(publisherUri.toString(), framedResultNode.at(PUBLISHER_ID_JSON_PTR).textValue());
        assertEquals(publisherName, framedResultNode.at(PUBLISHER_NAME_JSON_PTR).textValue());
        assertEquals(seriesUri.toString(), framedResultNode.at(SERIES_ID_JSON_PTR).textValue());
        assertEquals(seriesName, framedResultNode.at(SERIES_NAME_JSON_PTR).textValue());
    }

    @Test
    void shouldReturnDocumentWithIdBasedOnIdNameSpaceAndResourceIdentifier() throws JsonProcessingException {
        Publication publication = PublicationGenerator.randomPublication();
        var indexDocument = fromPublication(publication);
        assertThat(indexDocument.getId(), is(not(nullValue())));
        var documentId = indexDocument.getId();
        URI expectedUri = new UriWrapper(ID_NAMESPACE).addChild(publication.getIdentifier().toString()).getUri();
        assertThat(documentId, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnIndexDocumentContainingConfirmedSeriesUriFromNsdPublicationChannels()
        throws JsonProcessingException {
        Publication publication = randomBookWithConfirmedPublisher();
        Book book =
            extractBook(publication);
        Series series = (Series) book.getSeries();
        URI expectedSeriesUri = series.getId();
        IndexDocument actualDocument = fromPublication(publication);
        assertThat(actualDocument.getPublicationContextUris(), hasItems(expectedSeriesUri));
    }

    @Test
    void shouldReturnIndexDocumentContainingReturnsJournalUriFromNsdPublicationChannels()
        throws JsonProcessingException {
        Publication publication = randomJournalArticleWithConfirmedJournal();
        Journal journal =
            (Journal) publication.getEntityDescription().getReference().getPublicationContext();
        URI expectedJournalUri = journal.getId();
        IndexDocument actualDocument = fromPublication(publication);
        assertThat(actualDocument.getPublicationContextUris(), contains(expectedJournalUri));
    }

    @Test
    void shouldReturnIndexDocumentWithConfirmedSeriesIdWhenBookIsPartOfSeriesFoundInNsd()
        throws JsonProcessingException {
        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        IndexDocument actualDocument = fromPublication(publication);
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
        IndexDocument actualDocument = fromPublication(publication);
        Journal journal = extractJournal(publication);
        URI expectedJournalId = journal.getId();
        assertThat(actualDocument.getPublicationContextUris(), containsInAnyOrder(expectedJournalId));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationContext() throws JsonProcessingException {
        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().getReference().setPublicationContext(null);
        assertThat(IndexDocument.fromPublication(publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationInstance() throws JsonProcessingException {
        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().getReference().setPublicationInstance(null);
        assertThat(IndexDocument.fromPublication(publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoMainTitle() throws JsonProcessingException {
        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().setMainTitle(null);
        assertThat(IndexDocument.fromPublication(publication), is(not(nullValue())));
    }

    private static UriRetriever mockPublicationChannelPublisherResponse(URI journalId,
                                                                        String journalName,
                                                                        URI publisherId,
                                                                        String publisherName)
        throws IOException {
        final UriRetriever mockUriRetriever = mock(UriRetriever.class);
        String publicationChannelSampleJournal = getPublicationChannelSampleJournal(journalId, journalName);
        when(mockUriRetriever.getRawContent(eq(journalId), any()))
            .thenReturn(Optional.of(publicationChannelSampleJournal));
        String publicationChannelSamplePublisher = getPublicationChannelSamplePublisher(publisherId, publisherName);
        when(mockUriRetriever.getRawContent(eq(publisherId), any()))
            .thenReturn(Optional.of(publicationChannelSamplePublisher));
        return mockUriRetriever;
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
