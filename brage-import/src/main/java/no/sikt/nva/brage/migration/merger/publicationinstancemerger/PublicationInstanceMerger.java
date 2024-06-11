package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static java.util.Objects.nonNull;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.instancetypes.PublicationInstance;
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
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

public abstract class PublicationInstanceMerger<T extends PublicationInstance<?>> {

    protected final T publicationInstance;

    @JacocoGenerated
    public PublicationInstanceMerger(T publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public static PublicationInstanceMerger<?> of(PublicationInstance<?> instance) {
        return switch (instance) {
            case DegreePhd degreePhd -> new DegreePhdMerger(degreePhd);
            case DegreeBachelor degreeBachelor -> new DegreeBachelorMerger(degreeBachelor);
            case DegreeMaster degreeMaster -> new DegreeMasterMerger(degreeMaster);
            case DegreeLicentiate degreeLicentiate -> new DegreeLicentiateMerger(degreeLicentiate);
            case OtherStudentWork otherStudentWork -> new OtherStudentWorkMerger(otherStudentWork);
            case ConferenceReport conferenceReport -> new ConferenceReportMerger(conferenceReport);
            case ReportResearch reportResearch -> new ReportResearchMerger(reportResearch);
            case ReportWorkingPaper reportWorkingPaper -> new ReportWorkingPaperMerger(reportWorkingPaper);
            case ReportBookOfAbstract reportBookOfAbstract -> new ReportBookOfAbstractMerger(reportBookOfAbstract);
            case ReportBasic reportBasic -> new ReportBasicMerger(reportBasic);
            case JournalIssue journalIssue -> new JournalIssueMerger(journalIssue);
            case JournalLeader journalLeader -> new JournalLeaderMerger(journalLeader);
            case ProfessionalArticle professionalArticle -> new ProfessionalArticleMerger(professionalArticle);
            case AcademicArticle academicArticle -> new AcademicArticleMerger(academicArticle);
            case MediaFeatureArticle mediaFeatureArticle -> new MediaFeatureArticleMerger(mediaFeatureArticle);
            default -> new NoMerger<>(instance);
        };
    }

    public abstract T merge(PublicationInstance<?> newInstance);

    public static MonographPages getPages(MonographPages pages, MonographPages bragePages) {
        return nonNull(pages) ? pages : bragePages;
    }

    public static Range getRange(Range oldRange, Range newRange) {
        return nonNull(oldRange) ? oldRange : newRange;
    }

    public static PublicationDate getDate(PublicationDate submittedDate, PublicationDate brageDate) {
        return nonNull(submittedDate) ? submittedDate : brageDate;
    }

    public static String getNonNullValue(String oldValue, String newValue) {
        return nonNull(oldValue) ? oldValue : newValue;
    }
}
