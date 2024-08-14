package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtype;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalIssue;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import org.junit.jupiter.api.Test;
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
                         Arguments.of(new Map(null, null)),
                         Arguments.of(new OtherStudentWork(null, null)),
                         Arguments.of(new ConferenceReport(null)),
                         Arguments.of(new ReportResearch(null)),
                         Arguments.of(new ReportBasic(null)),
                         Arguments.of(new ReportWorkingPaper(null)),
                         Arguments.of(new ReportBookOfAbstract(null)),
                         Arguments.of(new JournalIssue(null, null, null, null)),
                         Arguments.of(new JournalLeader(null, null, null, null)),
                         Arguments.of(new ProfessionalArticle(null, null, null, null)),
                         Arguments.of(new AcademicArticle(null, null, null, null)),
                         Arguments.of(new MediaFeatureArticle(null, null, null, null))
        );
    }

    @ParameterizedTest
    @MethodSource("emptyPublicationInstanceSupplier")
    void shouldUseExistingPublicationInstanceWhenNewPublicationInstanceIsEmpty(
        PublicationInstance<?> publicationInstance) throws InvalidUnconfirmedSeriesException, InvalidIsbnException,
                                                           InvalidIssnException {
        var existingPublication = randomPublication(publicationInstance.getClass());
        var bragePublication = randomPublication(publicationInstance.getClass());
        bragePublication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValuesIgnoringFields(Set.of("duration")));
    }

    @ParameterizedTest
    @MethodSource("emptyPublicationInstanceSupplier")
    void shouldUseNewPublicationInstanceWhenExistingPublicationInstanceIsEmpty(
        PublicationInstance<?>  publicationInstance) throws InvalidUnconfirmedSeriesException, InvalidIsbnException,
                                                            InvalidIssnException {
        var existingPublication = randomPublication(publicationInstance.getClass());
        existingPublication.setAdditionalIdentifiers(Set.of());
        existingPublication.getEntityDescription().getReference().setPublicationInstance(publicationInstance);
        var bragePublication = randomPublication(publicationInstance.getClass());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldReturnInitialPublicationInstanceWhenInstanceTypeNotSupportedForMerge()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(MovingPicture.class);
        existingPublication.getEntityDescription().getReference().setPublicationInstance(new MovingPicture(
            MovingPictureSubtype.createOther(randomString()), randomString(), List.of(), null));
        var bragePublication = randomPublication(MovingPicture.class);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationInstance(),
                   is(equalTo(existingPublication.getEntityDescription().getReference().getPublicationInstance())));
    }

    private static Publication mergePublications(Publication existingPublication, Publication bragePublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var record = new Record();
        record.setId(bragePublication.getHandle());
        var representation = new PublicationRepresentation(record, bragePublication);
        return new CristinImportPublicationMerger(existingPublication, representation).mergePublications();
    }
}
