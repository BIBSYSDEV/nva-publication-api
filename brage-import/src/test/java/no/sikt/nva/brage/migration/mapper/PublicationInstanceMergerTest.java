package no.sikt.nva.brage.migration.mapper;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.stream.Stream;
import no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PublicationInstanceMergerTest {

    public static Stream<Arguments> emptyReportSupplier() {
        return Stream.of(Arguments.of(new ConferenceReport(null)),
                         Arguments.of(new ReportResearch(null)),
                         Arguments.of(new ReportBasic(null)),
                         Arguments.of(new ReportWorkingPaper(null)),
                         Arguments.of(new ReportBookOfAbstract(null)));
    }

    @ParameterizedTest
    @MethodSource("emptyReportSupplier")
    void shouldUseExistingPublicationInstanceWhenNewPublicationInstanceIsEmpty(PublicationInstance<?> publicationInstance)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(publicationInstance.getClass());
        var bragePublication = randomPublication(publicationInstance.getClass());
        bragePublication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    @ParameterizedTest
    @MethodSource("emptyReportSupplier")
    void shouldUseNewPublicationInstanceWhenExistingPublicationInstanceIsEmpty(PublicationInstance<?> publicationInstance)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
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
