package no.unit.nva.publication.doi.dto;

public enum PublicationType {
    JOURNAL_ARTICLE("JournalArticle"),
    JOURNAL_LETTER("JournalLetter"),
    JOURNAL_LEADER("JournalLeader"),
    JOURNAL_REVIEW("JournalReview"),
    JOURNAL_SHORT_COMMUNICATION("JournalShortCommunication"),
    BOOK_MONOGRAPH("BookMonograph"),
    BOOK_ANTHOLOGY("BookAnthology"),
    DEGREE_BACHELOR("DegreeBachelor"),
    DEGREE_MASTER("DegreeMaster"),
    DEGREE_PHD("DegreePhd"),
    REPORT("Report"),
    REPORT_POLICY("ReportPolicy"),
    REPORT_RESEARCH("ReportResearch"),
    REPORT_WORKING_PAPER("ReportWorkingPaper"),
    CHAPTER_ARTICLE("ChapterArticle");

    private final String type;

    PublicationType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
