package no.sikt.nva.brage.migration.mapper;

import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.BrageType;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import org.joda.time.DateTime;

public final class PublicationInstanceMapper {

    private PublicationInstanceMapper(){}

    public static PublicationInstance<? extends Pages> buildPublicationInstance(Record record) {
        if (isJournalArticle(record)) {
            return buildPublicationInstanceWhenJournalArticle(record);
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
        return null;
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
        return publicationInstance.getPages().getRange().getEnd();
    }

    private static String generateBegin(no.sikt.nva.brage.migration.record.PublicationInstance publicationInstance) {
        return publicationInstance.getPages().getRange().getBegin();
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
        return Optional.ofNullable(record.getEntityDescription().getPublicationInstance().getPages())
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
