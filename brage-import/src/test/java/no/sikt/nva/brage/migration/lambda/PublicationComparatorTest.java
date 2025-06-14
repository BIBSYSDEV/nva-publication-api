package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.lambda.PublicationComparator.publicationsMatch;
import static no.sikt.nva.brage.migration.lambda.PublicationComparator.publicationsMatchIgnoringTypeAndContributors;
import static no.unit.nva.model.testing.PublicationContextBuilder.randomPublicationContext;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Identity.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.pages.MonographPages;
import org.junit.jupiter.api.Test;

class PublicationComparatorTest {

    public static final String SENTENCE_STYLE_TITLE = "An integrative systematic review of promoting patient safety within prehospital emergency medical services by paramedics : A role theory perspective";
    public static final String HEADLINE_STYLE = "An Integrative Systematic Review of Promoting Patient Safety Within Prehospital Emergency Medical Services by Paramedics: A Role Theory Perspective";

    @Test
    void shouldReturnTrueWhenComparingBrageConferenceReportWithExistingLecture()
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        var existingLecture = randomPublication(Lecture.class);
        existingLecture.getEntityDescription().setPublicationDate(publicationDateWithYear());
        var incomingConferenceReport = existingLecture.copy()
                                           .withEntityDescription(createConferenceReport(existingLecture))
                                           .build();
        assertTrue(publicationsMatch(existingLecture, incomingConferenceReport));
    }

    @Test
    void shouldReturnTrueWhenComparingBrageConferenceReportWithExistingConferenceReport()
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        var existingLecture = randomPublication(ConferenceReport.class);
        existingLecture.getEntityDescription().setPublicationDate(publicationDateWithYear());
        var incomingConferenceReport = existingLecture.copy()
                                           .withEntityDescription(createConferenceReport(existingLecture))
                                           .build();
        assertTrue(publicationsMatch(existingLecture, incomingConferenceReport));
    }

    @Test
    void shouldReturnTrueWhenIncomingPublicationIsMissingPublicationContextAndPublicationInstance() {
        var existingPublication = randomPublication(Lecture.class);
        existingPublication.getEntityDescription().setPublicationDate(publicationDateWithYear());
        var incomingConferenceReport = existingPublication.copy()
                                           .withEntityDescription(addEmptyReference(existingPublication))
                                           .build();
        assertTrue(publicationsMatch(existingPublication, incomingConferenceReport));
    }

    @Test
    void shouldComparePublicationYearStringValuesWhenPublicationYearIsNotAnInteger() {
        var existingPublication = randomPublication(Lecture.class);
        existingPublication.getEntityDescription().setPublicationDate(publicationDateRandomString());
        var incomingConferenceReport = existingPublication.copy()
                                           .withEntityDescription(addEmptyReference(existingPublication))
                                           .build();
        assertTrue(publicationsMatch(existingPublication, incomingConferenceReport));
    }

    @Test
    void shouldReturnTrueWhenPublicationsTitleAreIdenticalButNotStyleTheyAreWrittenIn()
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        var existingLecture = randomPublication(Lecture.class);
        existingLecture.getEntityDescription().setMainTitle(SENTENCE_STYLE_TITLE);
        existingLecture.getEntityDescription().setPublicationDate(publicationDateWithYear());
        var incomingConferenceReport = existingLecture.copy()
                                           .withEntityDescription(createConferenceReport(existingLecture))
                                           .build();
        incomingConferenceReport.getEntityDescription().setMainTitle(HEADLINE_STYLE);
        assertTrue(publicationsMatch(existingLecture, incomingConferenceReport));
    }

    @Test
    void shouldReturnTrueWhenPublicationsHaveContributorsWithTheSameLastName() {
        var lastName = randomString();
        var existingPublication = randomPublication(Lecture.class);
        existingPublication.getEntityDescription().setContributors(List.of(contributorWithLastName(lastName)));
        var incomingPublication = existingPublication.copy()
                                      .withEntityDescription(existingPublication.getEntityDescription().copy()
                                                                 .withContributors(List.of(contributorWithLastName(lastName)))
                                                                 .build())
                                          .build();

        assertTrue(publicationsMatch(existingPublication, incomingPublication));
    }

    @Test
    void shouldReturnFalseWhenPublicationsHaveContributorsWithoutLastNames() {
        var existingPublication = randomPublication(Lecture.class);
        existingPublication.getEntityDescription().setContributors(List.of(contributorWithoutLastName()));
        var incomingPublication = existingPublication.copy()
                                      .withEntityDescription(existingPublication.getEntityDescription().copy()
                                                                 .withContributors(List.of(contributorWithoutLastName()))
                                                                 .build())
                                      .build();

        assertFalse(publicationsMatch(existingPublication, incomingPublication));
    }

    @Test
    void shouldReturnFalseWhenPublicationsHaveContributorsWithoutNames() {
        var existingPublication = randomPublication(Lecture.class);
        existingPublication.getEntityDescription().setContributors(List.of(contributorWithoutName()));
        var incomingPublication = existingPublication.copy()
                                      .withEntityDescription(existingPublication.getEntityDescription().copy()
                                                                 .withContributors(List.of(contributorWithoutName()))
                                                                 .build())
                                      .build();

        assertFalse(publicationsMatch(existingPublication, incomingPublication));
    }

    @Test
    void shouldReturnTrueWhenMergingJournalWithConfirmedJournalWithJournalWithUnconfirmedJournal() throws InvalidIssnException {
        var confirmedJournal = new Journal(randomUri());
        var unconfirmedJournal = new UnconfirmedJournal(randomString(), null, null);

        var publication = randomPublication(JournalArticle.class);

        var publicationWithConfirmedJournal = publication.copy().build();
        publicationWithConfirmedJournal.getEntityDescription().getReference().setPublicationContext(confirmedJournal);

        var publicationWithUnconfirmedJournal = publication.copy().build();
        publicationWithUnconfirmedJournal.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal);

        assertTrue(publicationsMatch(publicationWithConfirmedJournal,
                                                     publicationWithUnconfirmedJournal));
    }

    @Test
    void shouldReturnTrueWhenMergingJournalWithUnconfirmedJournalWithJournalWithConfirmedJournal() throws InvalidIssnException {
        var confirmedJournal = new Journal(randomUri());
        var unconfirmedJournal = new UnconfirmedJournal(randomString(), null, null);

        var publication = randomPublication(JournalArticle.class);

        var publicationWithConfirmedJournal = publication.copy().build();
        publicationWithConfirmedJournal.getEntityDescription().getReference().setPublicationContext(confirmedJournal);

        var publicationWithUnconfirmedJournal = publication.copy().build();
        publicationWithUnconfirmedJournal.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal);

        assertTrue(publicationsMatch(publicationWithUnconfirmedJournal,
                                     publicationWithConfirmedJournal));
    }

    @Test
    void shouldReturnTrueWhenComparingTwoEqualPublicationsWithDifferentContributorsAndPublicationContext() {
        var publication = randomPublication(JournalArticle.class);

        var academicMonograph = publication.copy().build();
        academicMonograph.getEntityDescription().getReference().setPublicationContext(randomPublicationContext(AcademicMonograph.class));
        academicMonograph.getEntityDescription().setContributors(List.of(contributorWithLastName(randomString())));

        assertTrue(publicationsMatchIgnoringTypeAndContributors(academicMonograph, publication));
    }

    private static Contributor contributorWithLastName(String lastName) {
        return new Contributor.Builder()
                   .withIdentity(identityWithLastName(lastName))
                   .build();
    }

    private static Contributor contributorWithoutName() {
        return new Contributor.Builder().build();
    }

    private static Contributor contributorWithoutLastName() {
        var identity = new Builder().withName(randomString()).build();
        return new Contributor.Builder()
            .withIdentity(identity)
            .build();
    }

    private static Identity identityWithLastName(String lastName) {
        return new Identity.Builder().withName(randomString() + " " + lastName).build();
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