package no.sikt.nva.brage.migration.mapper;

import static no.sikt.nva.brage.migration.mapper.BrageNvaMapper.extractDescription;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isBook;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isChapter;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isConferencePoster;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isCristinRecord;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isDataset;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isDesignProduct;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isFeatureArticle;
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
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArts;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtype;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
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
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
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
import org.joda.time.DateTime;

@SuppressWarnings("PMD.GodClass")
public final class PublicationInstanceMapper {

    private PublicationInstanceMapper() {
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.CognitiveComplexity", "PMD.NcssCount"})
    public static PublicationInstance<? extends Pages> buildPublicationInstance(Record record) {
        if (isJournalArticle(record)) {
            return buildPublicationInstanceWhenJournalArticle(record);
        }
        if (isScientificArticle(record)) {
            return buildPublicationInstanceWhenScientificArticle(record);
        }
        if (isFeatureArticle(record)) {
            return buildPublicationInstanceWhenFeatureArticle(record);
        }
        if (isMap(record)) {
            return buildPublicationInstanceWhenMap(record);
        }
        if (isDataset(record)) {
            return buildPublicationInstanceWhenDataset(record);
        }
        if (isChapter(record)) {
            return buildPublicationInstanceWhenChapter(record);
        }
        if (isScientificChapter(record)) {
            return buildPublicationInstanceWhenScientificChapter(record);
        }
        if (isLecture(record)) {
            return new Lecture();
        }
        if (isDesignProduct(record)) {
            return buildPublicationInstanceWhenDesignProduct();
        }
        if (isPlanOrBlueprint(record)) {
            return buildPublicationInstanceWhenPlanOrBluePrint();
        }
        if (isMusic(record)) {
            return buildPublicationInstanceWhenMusic();
        }
        if (isBachelorThesis(record)) {
            return buildPublicationInstanceWhenBachelorThesis(record);
        }
        if (isMasterThesis(record)) {
            return buildPublicationInstanceWhenMasterThesis(record);
        }
        if (isDoctoralThesis(record)) {
            return buildPublicationInstanceWhenDoctoralThesis(record);
        }
        if (isOtherStudentWork(record) || isStudentPaper(record)) {
            return buildPublicationInstanceWhenOtherStudentWork(record);
        }
        if (isBook(record)) {
            return buildPublicationInstanceWhenBook(record);
        }
        if (isScientificMonograph(record)) {
            return buildPublicationInstanceWhenScientificMonograph(record);
        }
        if (isResearchReport(record)) {
            return buildPublicationInstanceWhenResearchReport(record);
        }
        if (isConferencePoster(record)) {
            return buildPublicationInstanceWhenConferencePoster();
        }
        if (isConferenceReport(record)) {
            return buildPublicationInstanceWhenConferenceReport(record);
        }
        if (isReportWorkingPaper(record)) {
            return buildPublicationInstanceWhenReportWorkingPaper(record);
        }
        if (isInterview(record)) {
            return buildPublicationInstanceWhenMediaInterview();
        }
        if (isOtherPresentation(record)) {
           return buildPublicationInstanceWhenOtherPresentation();
        }
        if (isProfessionalArticle(record)) {
            return buildPublicationInstanceWhenProfessionalArticle(record);
        }
        if (isPerformingArts(record)) {
            return buildPublicationInstanceWhenPerformingArts();
        }
        if (isVisualArts(record)) {
            return buildPublicationInstanceWhenVisualArts();
        }
        if (isReaderOpinion(record)) {
            return buildPublicationInstanceWhenReaderOpinion(record);
        }
        if (isAnthology(record)) {
            return buildPublicationInstanceWhenAnthology(record);
        }
        if (isCristinRecord(record)) {
            return null;
        } else {
            return buildPublicationInstanceWhenReport(record);
        }
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenAnthology(Record record) {
        return new BookAnthology(extractMonographPages(record));
    }

    public static boolean isAnthology(Record record) {
        return NvaType.ANTHOLOGY.getValue().equals(record.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReaderOpinion(Record record) {
        return new MediaReaderOpinion(extractVolume(record), extractIssue(record), null, extractPages(record));
    }

    public static boolean isReaderOpinion(Record record) {
        return NvaType.READER_OPINION.getValue().equals(record.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenVisualArts() {
        return new VisualArts(VisualArtsSubtype.createOther(null), null, Set.of());
    }

    public static boolean isVisualArts(Record record) {
        return NvaType.VISUAL_ARTS.getValue().equals(record.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenPerformingArts() {
        return new PerformingArts(PerformingArtsSubtype.createOther(null), null, List.of());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenProfessionalArticle(Record record) {
        return new ProfessionalArticle(extractPages(record), extractVolume(record), extractIssue(record), null);
    }

    public static boolean isProfessionalArticle(Record record) {
        return NvaType.PROFESSIONAL_ARTICLE.getValue().equals(record.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenConferenceReport(Record record) {
        return new ConferenceReport(extractMonographPages(record));
    }

    public static boolean isConferenceReport(Record record) {
        return NvaType.CONFERENCE_REPORT.getValue().equals(record.getType().getNva());
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
        return new Architecture(ArchitectureSubtype.create(ArchitectureSubtypeEnum.OTHER),
                                null, Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenChapter(Record record) {
        return new NonFictionChapter(extractPages(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMusic() {
        return new MusicPerformance(Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDesignProduct() {
        return new ArtisticDesign(ArtisticDesignSubtype.create(ArtisticDesignSubtypeEnum.OTHER),
                                  null, Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificChapter(Record record) {
        return new AcademicChapter(extractPages(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenFeatureArticle(Record record) {
        return new FeatureArticle.Builder()
                   .withPages(extractPages(record))
                   .withIssue(extractIssue(record))
                   .withVolume(extractVolume(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReportWorkingPaper(Record record) {
        return new ReportWorkingPaper(extractMonographPages(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificArticle(Record record) {
        return new AcademicArticle(extractPages(record), extractVolume(record), extractIssue(record), null);
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificMonograph(Record record) {
        return new AcademicMonograph(extractMonographPages(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenOtherStudentWork(Record record) {
        return new OtherStudentWork(extractMonographPages(record), extractPublicationDate(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenResearchReport(Record record) {
        return new ReportResearch(extractMonographPages(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReport(Record record) {
        return new ReportBasic(extractMonographPages(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenBook(Record record) {
        return new NonFictionMonograph(extractMonographPages(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDataset(Record record) {
        return new DataSet(false, new GeographicalDescription(String.join(", ", extractSpatialCoverage(record))),
                           null, null, null);
    }

    private static String extractSpatialCoverage(Record record) {
        return Optional.ofNullable(record.getSpatialCoverage())
                   .map(spatialCoverages -> String.join(", ", spatialCoverages))
                   .orElse(null);
    }

    private static boolean isMap(Record record) {
        return NvaType.MAP.getValue().equals(record.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMap(Record record) {
        return new Map(extractDescription(record), extractMonographPages(record));
    }

    private static String extractPublicationYear(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationDate())
                   .map(PublicationInstanceMapper::generatePublicationYear)
                   .orElse(String.valueOf(DateTime.now().getYear()));
    }

    private static String extractPublicationDay(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationDate())
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

    private static boolean isDoctoralThesis(Record record) {
        return NvaType.DOCTORAL_THESIS.getValue().equals(record.getType().getNva());
    }

    private static boolean isBachelorThesis(Record record) {
        return NvaType.BACHELOR_THESIS.getValue().equals(record.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMasterThesis(Record record) {
        return new DegreeMaster(extractMonographPages(record), extractPublicationDate(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenBachelorThesis(Record record) {
        return new DegreeBachelor(extractMonographPages(record), extractPublicationDate(record));
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDoctoralThesis(Record record) {
        return new DegreePhd(extractMonographPages(record), extractPublicationDate(record));
    }

    private static MonographPages extractMonographPages(Record record) {
        return new MonographPages.Builder()
                   .withPages(extractPagesWhenMonographPages(record))
                   .build();
    }

    private static String extractPagesWhenMonographPages(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationInstance())
                   .map(no.sikt.nva.brage.migration.record.PublicationInstance::getPages)
                   .map(PublicationInstanceMapper::generatePages)
                   .orElse(null);
    }

    private static PublicationDate extractPublicationDate(Record record) {
        return new PublicationDate.Builder()
                   .withYear(extractPublicationYear(record))
                   .withMonth(extractPublicationMonth(record))
                   .withDay(extractPublicationDay(record))
                   .build();
    }

    private static String extractPublicationMonth(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationDate())
                   .map(PublicationInstanceMapper::generatePublicationMonth)
                   .orElse(null);
    }

    private static boolean isMasterThesis(Record record) {
        return NvaType.MASTER_THESIS.getValue().equals(record.getType().getNva());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenJournalArticle(Record record) {
        return new ProfessionalArticle(extractPages(record), extractVolume(record), extractIssue(record), null);
    }

    private static String extractVolume(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateVolume)
                   .orElse(null);
    }

    private static String extractIssue(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateIssue)
                   .orElse(null);
    }

    private static Range extractPages(Record record) {
        return new Range.Builder()
                   .withBegin(extractBeginValue(record))
                   .withEnd(extractEndValue(record))
                   .build();
    }

    private static String extractEndValue(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateEnd)
                   .orElse(null);
    }

    private static String extractBeginValue(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationInstance())
                   .map(PublicationInstanceMapper::generateBegin)
                   .orElse(null);
    }

    private static boolean isJournalArticle(Record record) {
        return NvaType.JOURNAL_ARTICLE.getValue().equals(record.getType().getNva());
    }
}
