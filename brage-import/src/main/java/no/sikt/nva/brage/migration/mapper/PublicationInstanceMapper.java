package no.sikt.nva.brage.migration.mapper;

import static no.sikt.nva.brage.migration.mapper.BrageNvaMapper.extractDescription;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.BrageType;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.BookMonographContentType;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportResearch;
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

    @SuppressWarnings("PMD.NPathComplexity")
    public static PublicationInstance<? extends Pages> buildPublicationInstance(Record record) {
        if (isJournalArticle(record)) {
            return buildPublicationInstanceWhenJournalArticle(record);
        }
        if (isMap(record)) {
            return buildPublicationInstanceWhenMap(record);
        }
        if (isDataset(record)) {
            return buildPublicationInstanceWhenDataset(record);
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
        if (isBook(record)) {
            return buildPublicationInstanceWhenBook(record);
        }
        if (isResearchReport(record)) {
            return buildPublicationInstanceWhenResearchReport(record);
        } else {
            return buildPublicationInstanceWhenReport(record);
        }
    }

    private static boolean isResearchReport(Record record) {
        return NvaType.RESEARCH_REPORT.getValue().equals(record.getType().getNva());
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

    private static boolean isBook(Record record) {
        return NvaType.BOOK.getValue().equals(record.getType().getNva());
    }

    private static boolean isDataset(Record record) {
        return NvaType.DATASET.getValue().equals(record.getType().getNva());
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

    private static boolean generatePeerReviewed(List<String> types) {
        return types.contains(BrageType.PEER_REVIEWED.getValue());
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
                   .withPeerReviewed(extractPeerReviewed(record))
                   .build();
    }

    private static boolean extractPeerReviewed(Record record) {
        return Optional.ofNullable(record.getType().getBrage())
                   .map(PublicationInstanceMapper::generatePeerReviewed)
                   .orElse(false);
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
