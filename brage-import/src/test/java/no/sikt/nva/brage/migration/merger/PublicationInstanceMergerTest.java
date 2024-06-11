package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.stream.Stream;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalIssue;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;
import no.unit.nva.model.pages.Range;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PublicationInstanceMergerTest {

    private static Publication mergePublications(Publication existingPublication, Publication bragePublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var record = new Record();
        record.setId(bragePublication.getHandle());
        var representation = new PublicationRepresentation(record, bragePublication);
        return new CristinImportPublicationMerger(existingPublication, representation).mergePublications();
    }

    public static Stream<Arguments> emptyJournalsSupplier() {
        return Stream.of(Arguments.of(new JournalIssue(null, null, null, null)),
                         Arguments.of(new JournalLeader(null, null, null, null)),
                         Arguments.of(new ProfessionalArticle(null, null, null, null)),
                         Arguments.of(new AcademicArticle(null, null, null, null)),
                         Arguments.of(new MediaFeatureArticle(null, null, null, null)));
    }

    @ParameterizedTest
    @MethodSource("emptyJournalsSupplier")
    void shouldUseExistingPublicationInstanceWhenNewPublicationInstanceIsEmpty(
        PublicationInstance<?> publicationInstance)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(publicationInstance.getClass());
        var bragePublication = randomPublication(publicationInstance.getClass());
        bragePublication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    @ParameterizedTest
    @MethodSource("emptyJournalsSupplier")
    void shouldUseNewPublicationInstanceWhenExistingPublicationInstanceIsEmpty(PublicationInstance<?> publicationInstance)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(publicationInstance.getClass());
        existingPublication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var bragePublication = randomPublication(publicationInstance.getClass());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }
}
