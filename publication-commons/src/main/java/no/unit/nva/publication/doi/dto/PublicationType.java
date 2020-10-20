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

    private final String name;

    PublicationType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Find PublicationType given json representation of enum.
     * @param name Json representation of PublicationType
     * @return PublicationType enum for given type or throws illegal argument exception.
     */
    public static PublicationType findByName(String name) {
        for (PublicationType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for: " + name);
    }
}
