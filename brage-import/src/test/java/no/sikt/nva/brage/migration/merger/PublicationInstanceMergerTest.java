package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PublicationInstanceMergerTest {

    public static Stream<Arguments> emptyPublicationInstanceSupplier() {
        return Stream.of(Arguments.of(new DegreePhd(null, null, Set.of())),
                         Arguments.of(new DegreeBachelor(null, null)),
                         Arguments.of(new DegreeMaster(null, null)),
                         Arguments.of(new DegreeLicentiate(null, null)),
                         Arguments.of(new OtherStudentWork(null, null)),
                         Arguments.of(new NonFictionMonograph(null)),
                         Arguments.of(new Textbook(null)),
                         Arguments.of(new DataSet(false, null, null, null, null)),
                         Arguments.of(new MusicPerformance(List.of(), null)),
                         Arguments.of(new MediaReaderOpinion(null, null, null, null)),
                         Arguments.of(new ExhibitionCatalog(null)),
                         Arguments.of(new Map(null, null)));
    }

    @ParameterizedTest
    @MethodSource("emptyPublicationInstanceSupplier")
    void shouldUseExistingPublicationInstanceWhenNewPublicationInstanceIsEmpty(PublicationInstance<?> publicationInstance)
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(publicationInstance.getClass());
        var bragePublication = randomPublication(publicationInstance.getClass());
        bragePublication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    @ParameterizedTest
    @MethodSource("emptyPublicationInstanceSupplier")
    void shouldUseNewPublicationInstanceWhenExistingPublicationInstanceIsEmpty(PublicationInstance<?> publicationInstance)
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(publicationInstance.getClass());
        existingPublication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var bragePublication = randomPublication(publicationInstance.getClass());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    private static Publication mergePublications(Publication existingPublication, Publication bragePublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var record = new Record();
        record.setId(bragePublication.getHandle());
        var representation = new PublicationRepresentation(record, bragePublication);
        return new CristinImportPublicationMerger(existingPublication, representation).mergePublications();
    }
}
