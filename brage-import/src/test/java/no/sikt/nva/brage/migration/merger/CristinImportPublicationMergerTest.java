package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import java.util.Set;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Book.BookBuilder;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
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
    void shouldFillExistingEmptyDegreePhdWithBrageDegreePhd() throws InvalidUnconfirmedSeriesException,
                                                                     InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(DegreePhd.class);
        existingPublication.getEntityDescription().getReference().setPublicationInstance(emptyDegreePhd());
        var bragePublication = randomPublication(DegreePhd.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingDegreeWhenUnsupportedContextType() throws InvalidUnconfirmedSeriesException,
                                                                     InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(DegreePhd.class);
        var bragePublication = randomPublication(DegreePhd.class);
        bragePublication.getEntityDescription().getReference().setPublicationInstance(null);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
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

    private static DegreePhd emptyDegreePhd() {
        return new DegreePhd(null, null, null);
    }
}