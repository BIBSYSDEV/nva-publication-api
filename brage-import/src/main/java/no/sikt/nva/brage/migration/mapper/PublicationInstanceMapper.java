package no.sikt.nva.brage.migration.mapper;

import static no.sikt.nva.brage.migration.mapper.BrageNvaMapper.extractDescription;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isBook;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isChapter;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isConferencePoster;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isDataset;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isDesignProduct;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isFeatureArticle;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isLecture;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isMusic;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isOtherStudentWork;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isPlanOrBlueprint;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isReportWorkingPaper;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isResearchReport;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isScientificArticle;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isScientificChapter;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isScientificMonograph;
import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.isStudentPaper;
import java.util.Collections;
import java.util.Optional;
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
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.BookMonographContentType;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.NullPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import org.joda.time.DateTime;

@SuppressWarnings("PMD.GodClass")
public final class PublicationInstanceMapper {

    private PublicationInstanceMapper() {
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.CognitiveComplexity"})
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
        if (isReportWorkingPaper(record)) {
            return buildPublicationInstanceWhenReportWorkingPaper(record);
        } else {
            return buildPublicationInstanceWhenReport(record);
        }
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenConferencePoster() {
        var poster = new ConferencePoster();
        poster.setPages(new NullPages());
        return poster;
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenPlanOrBluePrint() {
        return new Architecture(ArchitectureSubtype.create(ArchitectureSubtypeEnum.OTHER),
                                null, Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenChapter(Record record) {
        return new ChapterArticle.Builder()
                   .withPages(extractPages(record))
                   .withPeerReviewed(false)
                   .withContentType(ChapterArticleContentType.NON_FICTION_CHAPTER)
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenMusic() {
        return new MusicPerformance(Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDesignProduct() {
        return new ArtisticDesign(ArtisticDesignSubtype.create(ArtisticDesignSubtypeEnum.OTHER),
                                  null, Collections.emptyList());
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificChapter(Record record) {
        return new ChapterArticle.Builder()
                   .withPages(extractPages(record))
                   .withPeerReviewed(true)
                   .withContentType(ChapterArticleContentType.ACADEMIC_CHAPTER)
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenFeatureArticle(Record record) {
        return new FeatureArticle.Builder()
                   .withPages(extractPages(record))
                   .withIssue(extractIssue(record))
                   .withVolume(extractVolume(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReportWorkingPaper(Record record) {
        return new ReportWorkingPaper.Builder()
                   .withPages(extractMonographPages(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificArticle(Record record) {
        return new JournalArticle.Builder()
                   .withPages(extractPages(record))
                   .withIssue(extractIssue(record))
                   .withVolume(extractVolume(record))
                   .withPeerReviewed(true)
                   .withContent(JournalArticleContentType.ACADEMIC_ARTICLE)
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenScientificMonograph(Record record) {
        return new BookMonograph.Builder()
                   .withContentType(BookMonographContentType.ACADEMIC_MONOGRAPH)
                   .withPages(extractMonographPages(record))
                   .withPeerReviewed(true)
                   .withOriginalResearch(false)
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenOtherStudentWork(Record record) {
        return new OtherStudentWork.Builder()
                   .withPages(extractMonographPages(record))
                   .withSubmittedDate(extractPublicationDate(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenResearchReport(Record record) {
        return new ReportResearch.Builder()
                   .withPages(extractMonographPages(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenReport(Record record) {
        return new ReportBasic.Builder()
                   .withPages(extractMonographPages(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenBook(Record record) {
        return new BookMonograph.Builder()
                   .withContentType(BookMonographContentType.NON_FICTION_MONOGRAPH)
                   .withPages(extractMonographPages(record))
                   .build();
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
        return new DegreeMaster.Builder()
                   .withPages(extractMonographPages(record))
                   .withSubmittedDate(extractPublicationDate(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenBachelorThesis(Record record) {
        return new DegreeBachelor.Builder()
                   .withPages(extractMonographPages(record))
                   .withSubmittedDate(extractPublicationDate(record))
                   .build();
    }

    private static PublicationInstance<? extends Pages> buildPublicationInstanceWhenDoctoralThesis(Record record) {
        return new DegreePhd.Builder()
                   .withPages(extractMonographPages(record))
                   .withSubmittedDate(extractPublicationDate(record))
                   .build();
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
        return new JournalArticle.Builder()
                   .withPages(extractPages(record))
                   .withIssue(extractIssue(record))
                   .withVolume(extractVolume(record))
                   .withPeerReviewed(false)
                   .withContent(JournalArticleContentType.PROFESSIONAL_ARTICLE)
                   .build();
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
