package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Set;
import java.util.stream.Stream;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeBase;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PublicationInstanceMergerTest {

    public static Stream<Arguments> emptyDegreeSupplier() {
        return Stream.of(Arguments.of(new DegreePhd(null, null, Set.of())),
                         Arguments.of(new DegreeBachelor(null, null)),
                         Arguments.of(new DegreeMaster(null, null)),
                         Arguments.of(new DegreeLicentiate(null, null)),
                         Arguments.of(new OtherStudentWork(null, null)));
    }

    @ParameterizedTest
    @MethodSource("emptyDegreeSupplier")
    void shouldUseExistingPublicationInstanceWhenNewPublicationInstanceIsEmpty(DegreeBase degreeBase)
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(degreeBase.getClass());
        var bragePublication = randomPublication(degreeBase.getClass());
        bragePublication.getEntityDescription().getReference().setPublicationInstance(degreeBase);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    @ParameterizedTest
    @MethodSource("emptyDegreeSupplier")
    void shouldUseNewPublicationInstanceWhenExistingPublicationInstanceIsEmpty(DegreeBase degreeBase)
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(degreeBase.getClass());
        existingPublication.getEntityDescription().getReference().setPublicationInstance(degreeBase);
        var bragePublication = randomPublication(degreeBase.getClass());
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