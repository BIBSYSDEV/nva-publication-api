package no.unit.nva.publication;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PublicationGeneratorTest {

    public static final Set<String> FIELDS_EXPECTED_TO_BE_NULL = Set.of(".doiRequest");
    public static final Pattern EXPECTED_PUBLICATION_CHANNELS_URI =
        Pattern.compile("https://.*?nva\\.aws\\.unit\\.no/publication-channels/.*");

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    void shouldReturnPublicationWithoutEmptyFields(Class<?> publicationInstance) {
        assertThat(PublicationGenerator.randomPublication(publicationInstance),
                   doesNotHaveEmptyValuesIgnoringFields(FIELDS_EXPECTED_TO_BE_NULL));
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    void shouldReturnPublicationWithPublicationChannelUriWherePublicationChannelUriIsExpected(Class<?> instantType) {
        Publication publication = PublicationGenerator.randomPublication(instantType);
        var publicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        if (isGenerallyBook(publicationContext)) {
            assertThatPublisherAndSeriesArePublicationChannelsUris(publicationContext);
        }
        if (isGenerallyJournal(publicationContext)) {
            assertThatJournalIdIsPublicationChannelsUri(publicationContext);
        }
        if (isBookSeries(publicationContext)) {
            assertThatSeriesUriIsPublicationChannelsUri(publicationContext);
        }
    }

    private void assertThatSeriesUriIsPublicationChannelsUri(PublicationContext publicationContext) {
        Series series = (Series) publicationContext;
        assertThatUriPointsToPublicationChannelsService(series.getId());
    }

    private boolean isBookSeries(PublicationContext publicationContext) {
        return publicationContext instanceof BookSeries;
    }

    private void assertThatJournalIdIsPublicationChannelsUri(PublicationContext publicationContext) {
        Journal journal = (Journal) publicationContext;
        assertThatUriPointsToPublicationChannelsService(journal.getId());
    }

    private void assertThatUriPointsToPublicationChannelsService(URI id) {
        assertThat(id.toString(), matchesPattern(EXPECTED_PUBLICATION_CHANNELS_URI));
    }

    private boolean isGenerallyJournal(PublicationContext publicationContext) {
        return publicationContext instanceof Journal;
    }

    private void assertThatPublisherAndSeriesArePublicationChannelsUris(PublicationContext publicationContext) {
        Book book = (Book) publicationContext;
        Publisher publisher = (Publisher) book.getPublisher();
        Series series = (Series) book.getSeries();
        assertThatUriPointsToPublicationChannelsService(publisher.getId());
        assertThatUriPointsToPublicationChannelsService(series.getId());
    }

    private boolean isGenerallyBook(PublicationContext publicationContext) {
        // Book is supertype to all book-like contexts
        return publicationContext instanceof Book;
    }
}