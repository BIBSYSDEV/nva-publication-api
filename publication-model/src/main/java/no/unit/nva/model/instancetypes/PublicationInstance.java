package no.unit.nva.model.instancetypes;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.artistic.architecture.Architecture;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesign;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArts;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.Encyclopedia;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.PopularScienceMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.ChapterConferenceAbstract;
import no.unit.nva.model.instancetypes.chapter.ChapterInReport;
import no.unit.nva.model.instancetypes.chapter.EncyclopediaChapter;
import no.unit.nva.model.instancetypes.chapter.ExhibitionCatalogChapter;
import no.unit.nva.model.instancetypes.chapter.Introduction;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.chapter.PopularScienceChapter;
import no.unit.nva.model.instancetypes.chapter.TextbookChapter;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.event.ConferenceLecture;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.event.OtherPresentation;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.instancetypes.journal.CaseReport;
import no.unit.nva.model.instancetypes.journal.ConferenceAbstract;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalIssue;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.journal.PopularScienceArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.instancetypes.journal.StudyProtocol;
import no.unit.nva.model.instancetypes.media.MediaBlogPost;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.media.MediaParticipationInRadioOrTv;
import no.unit.nva.model.instancetypes.media.MediaPodcast;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;
import no.unit.nva.model.instancetypes.report.ReportPolicy;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.DataManagementPlan;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.pages.Pages;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Architecture", value = Architecture.class),
    @JsonSubTypes.Type(name = "ArtisticDesign", value = ArtisticDesign.class),
    @JsonSubTypes.Type(name = "MovingPicture", value = MovingPicture.class),
    @JsonSubTypes.Type(name = "PerformingArts", value = PerformingArts.class),
    @JsonSubTypes.Type(name = "AcademicArticle", value = AcademicArticle.class),
    @JsonSubTypes.Type(name = "AcademicLiteratureReview", value = AcademicLiteratureReview.class),
    @JsonSubTypes.Type(name = "CaseReport", value = CaseReport.class),
    @JsonSubTypes.Type(name = "StudyProtocol", value = StudyProtocol.class),
    @JsonSubTypes.Type(name = "ProfessionalArticle", value = ProfessionalArticle.class),
    @JsonSubTypes.Type(name = "PopularScienceArticle", value = PopularScienceArticle.class),
    @JsonSubTypes.Type(name = "JournalCorrigendum", value = JournalCorrigendum.class),
    @JsonSubTypes.Type(name = "JournalLetter", value = JournalLetter.class),
    @JsonSubTypes.Type(name = "JournalLeader", value = JournalLeader.class),
    @JsonSubTypes.Type(name = "JournalReview", value = JournalReview.class),
    @JsonSubTypes.Type(name = "AcademicMonograph", value = AcademicMonograph.class),
    @JsonSubTypes.Type(name = "PopularScienceMonograph", value = PopularScienceMonograph.class),
    @JsonSubTypes.Type(name = "Encyclopedia", value = Encyclopedia.class),
    @JsonSubTypes.Type(name = "ExhibitionCatalog", value = ExhibitionCatalog.class),
    @JsonSubTypes.Type(name = "NonFictionMonograph", value = NonFictionMonograph.class),
    @JsonSubTypes.Type(name = "Textbook", value = Textbook.class),
    @JsonSubTypes.Type(name = "BookAnthology", value = BookAnthology.class),
    @JsonSubTypes.Type(name = "DegreeBachelor", value = DegreeBachelor.class),
    @JsonSubTypes.Type(name = "DegreeMaster", value = DegreeMaster.class),
    @JsonSubTypes.Type(name = "DegreePhd", value = DegreePhd.class),
    @JsonSubTypes.Type(name = "DegreeLicentiate", value = DegreeLicentiate.class),
    @JsonSubTypes.Type(name = "ReportBasic", value = ReportBasic.class),
    @JsonSubTypes.Type(name = "ReportPolicy", value = ReportPolicy.class),
    @JsonSubTypes.Type(name = "ReportResearch", value = ReportResearch.class),
    @JsonSubTypes.Type(name = "ReportWorkingPaper", value = ReportWorkingPaper.class),
    @JsonSubTypes.Type(name = "ConferenceReport", value = ConferenceReport.class),
    @JsonSubTypes.Type(name = "ReportBookOfAbstract", value = ReportBookOfAbstract.class),
    @JsonSubTypes.Type(name = "AcademicChapter", value = AcademicChapter.class),
    @JsonSubTypes.Type(name = "EncyclopediaChapter", value = EncyclopediaChapter.class),
    @JsonSubTypes.Type(name = "ExhibitionCatalogChapter", value = ExhibitionCatalogChapter.class),
    @JsonSubTypes.Type(name = "Introduction", value = Introduction.class),
    @JsonSubTypes.Type(name = "NonFictionChapter", value = NonFictionChapter.class),
    @JsonSubTypes.Type(name = "PopularScienceChapter", value = PopularScienceChapter.class),
    @JsonSubTypes.Type(name = "TextbookChapter", value = TextbookChapter.class),
    @JsonSubTypes.Type(name = "ChapterConferenceAbstract", value = ChapterConferenceAbstract.class),
    @JsonSubTypes.Type(name = "ChapterInReport", value = ChapterInReport.class),
    @JsonSubTypes.Type(name = "OtherStudentWork", value = OtherStudentWork.class),
    @JsonSubTypes.Type(name = "ConferenceLecture", value = ConferenceLecture.class),
    @JsonSubTypes.Type(name = "ConferencePoster", value = ConferencePoster.class),
    @JsonSubTypes.Type(name = "Lecture", value = Lecture.class),
    @JsonSubTypes.Type(name = "OtherPresentation", value = OtherPresentation.class),
    @JsonSubTypes.Type(name = "JournalIssue", value = JournalIssue.class),
    @JsonSubTypes.Type(name = "ConferenceAbstract", value = ConferenceAbstract.class),
    @JsonSubTypes.Type(name = "MediaFeatureArticle", value = MediaFeatureArticle.class),
    @JsonSubTypes.Type(name = "MediaBlogPost", value = MediaBlogPost.class),
    @JsonSubTypes.Type(name = "MediaInterview", value = MediaInterview.class),
    @JsonSubTypes.Type(name = "MediaParticipationInRadioOrTv", value = MediaParticipationInRadioOrTv.class),
    @JsonSubTypes.Type(name = "MediaPodcast", value = MediaPodcast.class),
    @JsonSubTypes.Type(name = "MediaReaderOpinion", value = MediaReaderOpinion.class),
    @JsonSubTypes.Type(name = "MusicPerformance", value = MusicPerformance.class),
    @JsonSubTypes.Type(name = "DataManagementPlan", value = DataManagementPlan.class),
    @JsonSubTypes.Type(name = "DataSet", value = DataSet.class),
    @JsonSubTypes.Type(name = "VisualArts", value = VisualArts.class),
    @JsonSubTypes.Type(name = "Map", value = Map.class),
    @JsonSubTypes.Type(name = "LiteraryArts", value = LiteraryArts.class),
    @JsonSubTypes.Type(name = "ExhibitionProduction", value = ExhibitionProduction.class)
})
public interface PublicationInstance<P extends Pages> {


    @JsonProperty(PAGES_FIELD)
    P getPages();

    @JsonIgnore
    default String getInstanceType() {
        return this.getClass().getSimpleName();
    }

    class Constants {
        public static final String  PAGES_FIELD = "pages";
    }
}
