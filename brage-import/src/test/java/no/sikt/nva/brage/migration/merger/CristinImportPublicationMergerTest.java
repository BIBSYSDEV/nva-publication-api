package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Set;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import org.junit.jupiter.api.Test;

class CristinImportPublicationMergerTest {

    @Test
    void shouldUseBrageBookWhenExistingBookIsEmpty() throws InvalidUnconfirmedSeriesException, InvalidIsbnException {
        var existingPublication = randomPublication(BookAnthology.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyBook());
        var bragePublication = randomPublication(BookAnthology.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValuesIgnoringFields(Set.of("additionalIdentifiers")));
    }

    @Test
    void shouldUseExistingBookWhenBrageBookIsEmpty() throws InvalidUnconfirmedSeriesException, InvalidIsbnException {
        var bragePublication = randomPublication(BookAnthology.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyBook());
        var existingPublication = randomPublication(BookAnthology.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValuesIgnoringFields(Set.of("additionalIdentifiers")));
    }

    @Test
    void shouldFillExistingEmptyDegreePhdWithBrageDegreePhd() throws InvalidUnconfirmedSeriesException,
                                                                InvalidIsbnException {
        var existingPublication = randomPublication(DegreePhd.class);
        existingPublication.getEntityDescription().getReference().setPublicationInstance(emptyDegreePhd());
        var bragePublication = randomPublication(DegreePhd.class);

        var updatedPublication = mergePublications(bragePublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    private static Publication mergePublications(Publication existingPublication, Publication bragePublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var record = new Record();
        record.setId(bragePublication.getHandle());
        var representation = new PublicationRepresentation(record, bragePublication);
        return new CristinImportPublicationMerger(existingPublication,
                                                  representation).mergePublications();
    }

    private static Book emptyBook() throws InvalidUnconfirmedSeriesException {
        return new Book(null, null, null, null, null, null);
    }

    private static DegreePhd emptyDegreePhd() {
        return new DegreePhd(null, null, null);
    }
}