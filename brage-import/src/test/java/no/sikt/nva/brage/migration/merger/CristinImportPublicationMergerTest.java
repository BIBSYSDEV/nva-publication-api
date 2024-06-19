package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.util.List;
import java.util.Set;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Book.BookBuilder;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.NullPublisher;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import org.junit.jupiter.api.Test;

class CristinImportPublicationMergerTest {

    @Test
    void shouldUseBrageBookWhenExistingBookIsEmpty()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(BookAnthology.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyBook());
        var bragePublication = randomPublication(BookAnthology.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValuesIgnoringFields(Set.of("additionalIdentifiers")));
    }

    @Test
    void shouldUseExistingBookWhenBrageBookIsEmpty()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var bragePublication = randomPublication(BookAnthology.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyBook());
        var existingPublication = randomPublication(BookAnthology.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValuesIgnoringFields(Set.of("additionalIdentifiers")));
    }

    @Test
    void shouldUseBrageJournalWhenExistingJournalIsUnconfirmedJournalMissingAllValues() throws InvalidUnconfirmedSeriesException,
                                                                    InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyJournal());
        var bragePublication = randomPublication(JournalArticle.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingJournalWhenExistingJournalIsJournalAndBragePublicationHasUnconfirmedJournal()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(JournalArticle.class);
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(Journal.class)));
    }

    @Test
    void shouldUseExistingJournalWhenExistingJournalIsUnconfirmedJournalAndBragePublicationHasUnconfirmedJournalWithNullValues()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyUnconfirmedJournal());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(UnconfirmedJournal.class)));
        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldReturnExistingJournalWhenNewPublicationContextIsNotJournal()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(UnconfirmedJournal.class)));
        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldReturnExistingJournalWhenNewPublicationContextIsNotSupportedJournalType()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new BookBuilder().build());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(UnconfirmedJournal.class)));
        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingReportIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(ReportResearch.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyReport());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingEventIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(Lecture.class);
        var bragePublication = randomPublication(Lecture.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyEvent());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingAnthologyIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(ChapterArticle.class);
        var bragePublication = randomPublication(ChapterArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new Anthology());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingMediaContributionIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(MediaInterview.class);
        var bragePublication = randomPublication(MediaInterview.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new Anthology());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingResearchDataIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DataSet.class);
        var bragePublication = randomPublication(DataSet.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new ResearchData(new NullPublisher()));
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        var researchData = (ResearchData) updatedPublication.getEntityDescription().getReference().getPublicationContext();
        assertThat(researchData.getPublisher(), is(instanceOf(Publisher.class)));
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingGeographicalContentIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(Map.class);
        var bragePublication = randomPublication(Map.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new GeographicalContent(new NullPublisher()));
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        var researchData = (GeographicalContent) updatedPublication.getEntityDescription().getReference().getPublicationContext();
        assertThat(researchData.getPublisher(), is(instanceOf(Publisher.class)));
    }

    @Test
    void shouldUseIncomingPublicationsFundingsWhenExistingPublicationMissesProjects()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(Map.class);
        existingPublication.setFundings(List.of());
        var bragePublication = randomPublication(Map.class);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getProjects(), is(not(emptyIterable())));
    }

    private PublicationContext emptyUnconfirmedJournal() throws InvalidIssnException {
        return new UnconfirmedJournal(null, null, null);
    }

    private static Publication mergePublications(Publication existingPublication, Publication bragePublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var record = new Record();
        record.setId(bragePublication.getHandle());
        var representation = new PublicationRepresentation(record, bragePublication);
        return new CristinImportPublicationMerger(existingPublication,
                                                  representation).mergePublications();
    }

    private static Book emptyBook() throws InvalidUnconfirmedSeriesException {
        return new Book(null, null, null, null, null, null);
    }

    private static UnconfirmedJournal emptyJournal() throws InvalidIssnException {
        return new UnconfirmedJournal(null, null, null);
    }

    private static UnconfirmedJournal unconfirmedJournal() throws InvalidIssnException {
        return new UnconfirmedJournal(randomString(), randomIssn(), randomIssn());
    }

    private static Report emptyReport() throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder().build();
    }

    private static Event emptyEvent() {
        return new Event.Builder().build();
    }

}