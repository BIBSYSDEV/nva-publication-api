package no.sikt.nva.brage.migration.mapper;

import static no.sikt.nva.brage.migration.mapper.BrageNvaMapper.extractDescription;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isBook;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isChapter;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isConferencePoster;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isCristinRecord;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isDataset;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isDesignProduct;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isMediaFeatureArticle;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isFilm;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isInterview;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isLecture;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isMusic;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isOtherPresentation;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isOtherStudentWork;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isPerformingArts;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isPlanOrBlueprint;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isReportWorkingPaper;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isResearchReport;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isScientificArticle;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isScientificChapter;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isScientificMonograph;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isStudentPaper;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isTextbook;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.architecture.Architecture;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureSubtype;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesign;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesignSubtype;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesignSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtype;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArts;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArtsSubtypeOther;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArts;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtype;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.event.OtherPresentation;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

@SuppressWarnings("PMD.GodClass")
public final class PublicationInstanceMapper {

    private PublicationInstanceMapper() {
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.CognitiveComplexity", "PMD.NcssCount"})
    public static PublicationInstance<? extends Pages> buildPublicationInstance(Record brageRecord) {
        if (isJournalArticle(brageRecord)) {
            return buildPublicationInstanceWhenJournalArticle(brageRecord);
        }
        if (isScientificArticle(brageRecord)) {
            return buildPublicationInstanceWhenScientificArticle(brageRecord);
        }
        if (isMediaFeatureArticle(brageRecord)) {
            return buildPublicationInstanceWhenMediaFeatureArticle(brageRecord);
        }
        if (isMap(brageRecord)) {
            return buildPublicationInstanceWhenMap(brageRecord);
        }
        if (isDataset(brageRecord)) {
            return buildPublicationInstanceWhenDataset(brageRecord);
        }
        if (isChapter(brageRecord)) {
            return buildPublicationInstanceWhenChapter(brageRecord);
        }
        if (isScientificChapter(brageRecord)) {
            return buildPublicationInstanceWhenScientificChapter(brageRecord);
        }
        if (isLecture(brageRecord)) {
            return new Lecture();
        }
        if (isDesignProduct(brageRecord)) {
            return buildPublicationInstanceWhenDesignProduct();
        }
        if (isPlanOrBlueprint(brageRecord)) {
            return buildPublicationInstanceWhenPlanOrBluePrint();
        }
        if (isMusic(brageRecord)) {
            return buildPublicationInstanceWhenMusic();
        }
        if (isBachelorThesis(brageRecord)) {
            return buildPublicationInstanceWhenBachelorThesis(brageRecord);
        }
        if (isMasterThesis(brageRecord)) {
            return buildPublicationInstanceWhenMasterThesis(brageRecord);
        }
        if (isDoctoralThesis(brageRecord)) {
            return buildPublicationInstanceWhenDoctoralThesis(brageRecord);
        }
        if (isOtherStudentWork(brageRecord) || isStudentPaper(brageRecord)) {
            return buildPublicationInstanceWhenOtherStudentWork(brageRecord);
        }
        if (isBook(brageRecord)) {
            return buildPublicationInstanceWhenBook(brageRecord);
        }
        if (isScientificMonograph(brageRecord)) {
            return buildPublicationInstanceWhenScientificMonograph(brageRecord);
        }
        if (isResearchReport(brageRecord)) {
            return buildPublicationInstanceWhenResearchReport(brageRecord);
        }
        if (isConferencePoster(brageRecord)) {
            return buildPublicationInstanceWhenConferencePoster();
        }
        if (isConferenceReport(brageRecord)) {
            return buildPublicationInstanceWhenConferenceReport(brageRecord);
        }
        if (isReportWorkingPaper(brageRecord)) {
            return buildPublicationInstanceWhenReportWorkingPaper(brageRecord);
        }
        if (isInterview(brageRecord)) {
            return buildPublicationInstanceWhenMediaInterview();
        }
        if (isOtherPresentation(brageRecord)) {
            return buildPublicationInstanceWhenOtherPresentation();
        }
        if (isProfessionalArticle(brageRecord)) {
            return buildPublicationInstanceWhenProfessionalArticle(brageRecord);
        }
        if (isPerformingArts(brageRecord)) {
            return buildPublicationInstanceWhenPerformingArts();
        }
        if (isVisualArts(brageRecord)) {
            return buildPublicationInstanceWhenVisualArts();
        }
        if (isReaderOpinion(brageRecord)) {
            return buildPublicationInstanceWhenReaderOpinion(brageRecord);
        }
        if (isAnthology(brageRecord)) {
            return buildPublicationInstanceWhenAnthology(brageRecord);
        }
        if (isTextbook(brageRecord)) {
            return buildPublicationInstanceWhenTextbook(brageRecord);
        }
        if (isFilm(brageRecord)) {
            return buildPublicationInstanceWhenFilm();
        }
        if (isLiteraryArts(brageRecord)) {
            return buildPublicationInstanceWhenLiteraryArts();
        }
        if (isExhibitionCatalog(brageRecord)) {
            return buildPublicationInstanceWhenExhibitionCatalog(brageRecord);
        }
        if (isPopularScienceMonograph(brageRecord)) {
            return buildPublicationInstanceWhenExhibitionCatalog(brageRecord);
        }
        if (isEditorial(brageRecord)) {
            return buildPublicationInstanceWhenEditorial(brageRecord);
        }
        if (isCristinRecord(brageRecord)) {
            return null;
        } else {
            return buildPublicationInstanceWhenReport(brageRecord);
        }
    }

    public static boolean isEditorial(Record brageRecord) {
        return NvaType.EDITORIAL.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isPopularScienceMonograph(Record brageRecord) {
        return NvaType.POPULAR_SCIENCE_MONOGRAPH.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isExhibitionCatalog(Record brageRecord) {
        return NvaType.EXHIBITION_CATALOGUE.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isLiteraryArts(Record brageRecord) {
        return NvaType.LITERARY_ARTS.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isAnthology(Record brageRecord) {
        return NvaType.ANTHOLOGY.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isReaderOpinion(Record brageRecord) {
        return NvaType.READER_OPINION.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isVisualArts(Record brageRecord) {
        return NvaType.VISUAL_ARTS.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isProfessionalArticle(Record brageRecord) {
        return NvaType.PROFESSIONAL_ARTICLE.getValue().equals(brageRecord.getType().getNva());
    }

    public static boolean isConferenceReport(Record brageRecord) {
        return NvaType.CONFERENCE_REPORT.getValue().equals(brageRecord.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenEditorial(Record brageRecord) {
        return new JournalLeader(extractVolume(brageRecord), extractIssue(brageRecord),
                                 extractArticleNumber(brageRecord), extractPages(brageRecord));
    }

    private static String extractArticleNumber(Record brageRecord) {
        return brageRecord.getEntityDescription().getPublicationInstance().getArticleNumber();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenExhibitionCatalog(
        Record brageRecord) {
        return new ExhibitionCatalog(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenLiteraryArts() {
        return new LiteraryArts(LiteraryArtsSubtypeOther.createOther(null), List.of(), null);
    }

    @NotNull
    private static MovingPicture buildPublicationInstanceWhenFilm() {
        return new MovingPicture(MovingPictureSubtype.createOther(null), null, List.of());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenTextbook(Record brageRecord) {
        return new Textbook(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenAnthology(Record brageRecord) {
        return new BookAnthology(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReaderOpinion(Record brageRecord) {
        return new MediaReaderOpinion(extractVolume(brageRecord), extractIssue(brageRecord), null,
                                      extractPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenVisualArts() {
        return new VisualArts(VisualArtsSubtype.createOther(null), null, Set.of());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenPerformingArts() {
        return new PerformingArts(PerformingArtsSubtype.createOther(null), null, List.of());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenProfessionalArticle(
        Record brageRecord) {
        return new ProfessionalArticle(extractPages(brageRecord), extractVolume(brageRecord), extractIssue(brageRecord),
                                       null);
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenConferenceReport(
        Record brageRecord) {
        return new ConferenceReport(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenOtherPresentation() {
        return new OtherPresentation();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMediaInterview() {
        return new MediaInterview();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenConferencePoster() {
        return new ConferencePoster();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenPlanOrBluePrint() {
        return new Architecture(ArchitectureSubtype.create(ArchitectureSubtypeEnum.OTHER), null,
                                Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenChapter(Record brageRecord) {
        return new NonFictionChapter(extractPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMusic() {
        return new MusicPerformance(Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDesignProduct() {
        return new ArtisticDesign(ArtisticDesignSubtype.create(ArtisticDesignSubtypeEnum.OTHER), null,
                                  Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificChapter(
        Record brageRecord) {
        return new AcademicChapter(extractPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMediaFeatureArticle(Record brageRecord) {
        return new MediaFeatureArticle(extractVolume(brageRecord), extractIssue(brageRecord),
                                       extractArticleNumber(brageRecord), extractPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReportWorkingPaper(
        Record brageRecord) {
        return new ReportWorkingPaper(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificArticle(
        Record brageRecord) {
        return new AcademicArticle(extractPages(brageRecord), extractVolume(brageRecord), extractIssue(brageRecord),
                                   null);
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificMonograph(
        Record brageRecord) {
        return new AcademicMonograph(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenOtherStudentWork(
        Record brageRecord) {
        return new OtherStudentWork(extractMonographPages(brageRecord), extractPublicationDate(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenResearchReport(Record brageRecord) {
        return new ReportResearch(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReport(Record brageRecord) {
        return new ReportBasic(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenBook(Record brageRecord) {
        return new NonFictionMonograph(extractMonographPages(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDataset(Record brageRecord) {
        return new DataSet(false, new GeographicalDescription(String.join(", ", extractSpatialCoverage(brageRecord))),
                           null, null, null);
    }

    private static String extractSpatialCoverage(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getSpatialCoverage())
                   .map(spatialCoverages -> String.join(", ", spatialCoverages))
                   .orElse(null);
    }

    private static boolean isMap(Record brageRecord) {
        return NvaType.MAP.getValue().equals(brageRecord.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMap(Record brageRecord) {
        return new Map(extractDescription(brageRecord), extractMonographPages(brageRecord));
    }

    private static String extractPublicationYear(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationDate())
                   .map(PublicationInstanceMapper::generatePublicationYear)
                   .orElse(String.valueOf(DateTime.now().getYear()));
    }

    private static String extractPublicationDay(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationDate())
                   .map(PublicationInstanceMapper::generatePublicationDay)
                   .orElse(null);
    }

    private static String generatePublicationDay(no.sikt.nva.brage.migration.record.PublicationDate publicationDate) {
        return publicationDate.getNva().getDay();
    }

    private static String generatePublicationMonth(no.sikt.nva.brage.migration.record.PublicationDate publicationDate) {
        return publicationDate.getNva().getMonth();
    }

    private static String generatePublicationYear(no.sikt.nva.brage.migration.record.PublicationDate publicationDate) {
        return publicationDate.getNva().getYear();
    }

    private static String generateIssue(no.sikt.nva.brage.migration.record.PublicationInstance publicationInstance) {
        return publicationInstance.getIssue();
    }

    private static String generateEnd(no.sikt.nva.brage.migration.record.PublicationInstance publicationInstance) {
        return Optional.ofNullable(publicationInstance.getPages())
                   .map(no.sikt.nva.brage.migration.record.Pages::getRange)
                   .map(no.sikt.nva.brage.migration.record.Range::getEnd)
                   .orElse(null);
    }

    private static String generateBegin(no.sikt.nva.brage.migration.record.PublicationInstance publicationInstance) {
        return Optional.ofNullable(publicationInstance.getPages())
                   .map(no.sikt.nva.brage.migration.record.Pages::getRange)
                   .map(no.sikt.nva.brage.migration.record.Range::getBegin)
                   .orElse(null);
    }

    private static String generateVolume(no.sikt.nva.brage.migration.record.PublicationInstance publicationInstance) {
        return publicationInstance.getVolume();
    }

    private static String generatePages(no.sikt.nva.brage.migration.record.Pages pages) {
        return pages.getPages();
    }

    private static boolean isDoctoralThesis(Record brageRecord) {
        return NvaType.DOCTORAL_THESIS.getValue().equals(brageRecord.getType().getNva());
    }

    private static boolean isBachelorThesis(Record brageRecord) {
        return NvaType.BACHELOR_THESIS.getValue().equals(brageRecord.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMasterThesis(Record brageRecord) {
        return new DegreeMaster(extractMonographPages(brageRecord), extractPublicationDate(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenBachelorThesis(Record brageRecord) {
        return new DegreeBachelor(extractMonographPages(brageRecord), extractPublicationDate(brageRecord));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDoctoralThesis(Record brageRecord) {
        return new DegreePhd(extractMonographPages(brageRecord), extractPublicationDate(brageRecord));
    }

    private static MonographPages extractMonographPages(Record brageRecord) {
        return new MonographPages.Builder().withPages(extractPagesWhenMonographPages(brageRecord)).build();
    }

    private static String extractPagesWhenMonographPages(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationInstance())
                   .map(no.sikt.nva.brage.migration.record.PublicationInstance::getPages)
                   .map(PublicationInstanceMapper::generatePages)
                   .orElse(null);
    }

    private static PublicationDate extractPublicationDate(Record brageRecord) {
        return new PublicationDate.Builder().withYear(extractPublicationYear(brageRecord))
                   .withMonth(extractPublicationMonth(brageRecord))
                   .withDay(extractPublicationDay(brageRecord))
                   .build();
    }

    private static String extractPublicationMonth(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationDate())
                   .map(PublicationInstanceMapper::generatePublicationMonth)
                   .orElse(null);
    }

    private static boolean isMasterThesis(Record brageRecord) {
        return NvaType.MASTER_THESIS.getValue().equals(brageRecord.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenJournalArticle(Record brageRecord) {
        return new ProfessionalArticle(extractPages(brageRecord), extractVolume(brageRecord), extractIssue(brageRecord),
                                       null);
    }

    private static String extractVolume(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateVolume)
                   .orElse(null);
    }

    private static String extractIssue(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateIssue)
                   .orElse(null);
    }

    private static Range extractPages(Record brageRecord) {
        return new Range.Builder().withBegin(extractBeginValue(brageRecord))
                   .withEnd(extractEndValue(brageRecord))
                   .build();
    }

    private static String extractEndValue(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateEnd)
                   .orElse(null);
    }

    private static String extractBeginValue(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateBegin)
                   .orElse(null);
    }

    private static boolean isJournalArticle(Record brageRecord) {
        return NvaType.JOURNAL_ARTICLE.getValue().equals(brageRecord.getType().getNva());
    }
}
