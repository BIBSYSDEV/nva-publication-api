package no.sikt.nva.brage.migration.lambda;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.pages.MonographPages;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class PublicationComparatorTest {

    @Test
    void shouldReturnTrueWhenComparingBrageConferenceReportWithExistingLecture()
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        var existingLecture = randomPublication(Lecture.class);
        existingLecture.getEntityDescription().setPublicationDate(publicationDateWithYear());
        var incomingConferenceReport = existingLecture.copy()
                                           .withEntityDescription(createConferenceReport(existingLecture))
                                           .build();
        assertTrue(PublicationComparator.publicationsMatch(existingLecture, incomingConferenceReport));
    }

    @Test
    void shouldReturnTrueWhenComparingBrageConferenceReportWithExistingConferenceReport()
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        var existingLecture = randomPublication(ConferenceReport.class);
        existingLecture.getEntityDescription().setPublicationDate(publicationDateWithYear());
        var incomingConferenceReport = existingLecture.copy()
                                           .withEntityDescription(createConferenceReport(existingLecture))
                                           .build();
        assertTrue(PublicationComparator.publicationsMatch(existingLecture, incomingConferenceReport));
    }

    @Test
    void shouldReturnTrueWhenIncomingPublicationIsMissingPublicationContextAndPublicationInstance() {
        var existingPublication = randomPublication(Lecture.class);
        existingPublication.getEntityDescription().setPublicationDate(publicationDateWithYear());
        var incomingConferenceReport = existingPublication.copy()
                                           .withEntityDescription(addEmptyReference(existingPublication))
                                           .build();
        assertTrue(PublicationComparator.publicationsMatch(existingPublication, incomingConferenceReport));
    }

    @Test
    void shouldComparePublicationYearStringValuesWhenPublicationYearIsNotAnInteger() {
        var existingPublication = randomPublication(Lecture.class);
        existingPublication.getEntityDescription().setPublicationDate(publicationDateRandomString());
        var incomingConferenceReport = existingPublication.copy()
                                           .withEntityDescription(addEmptyReference(existingPublication))
                                           .build();
        assertTrue(PublicationComparator.publicationsMatch(existingPublication, incomingConferenceReport));
    }

    private static EntityDescription addEmptyReference(Publication existingPublication) {
        return existingPublication
                   .getEntityDescription().copy()
                   .withReference(new Reference.Builder().build())
                   .build();
    }

    private static PublicationDate publicationDateWithYear() {
        return new PublicationDate.Builder()
                   .withYear("2022")
                   .build();
    }

    private static PublicationDate publicationDateRandomString() {
        return new PublicationDate.Builder()
                   .withYear(randomString())
                   .build();
    }

    private EntityDescription createConferenceReport(Publication publication)
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        var entityDescription = publication.getEntityDescription();
        var conferenceReport = new ConferenceReport(new MonographPages.Builder().build());
        var report = new Report.Builder().build();
        return entityDescription.copy()
                   .withReference(new Reference.Builder()
                                      .withPublicationInstance(conferenceReport)
                                      .withPublishingContext(report)
                                      .build()).build();
    }
}