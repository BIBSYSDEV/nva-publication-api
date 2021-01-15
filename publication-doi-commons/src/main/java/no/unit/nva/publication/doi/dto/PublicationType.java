package no.unit.nva.publication.doi.dto;

public enum PublicationType {
    BOOK_ANTHOLOGY("BookAnthology"),
    BOOK_MONOGRAPH("BookMonograph"),
    CARTOGRAPHIC_MAP("CartographicMap"),
    CHAPTER_ARTICLE("ChapterArticle"),
    DEGREE_BACHELOR("DegreeBachelor"),
    DEGREE_MASTER("DegreeMaster"),
    DEGREE_PHD("DegreePhd"),
    FEATURE_ARTICLE("FeatureArticle"),
    JOURNAL_ARTICLE("JournalArticle"),
    JOURNAL_CORRIGENDUM("JournalCorrigendum"),
    JOURNAL_LEADER("JournalLeader"),
    JOURNAL_LETTER("JournalLetter"),
    JOURNAL_REVIEW("JournalReview"),
    JOURNAL_SHORT_COMMUNICATION("JournalShortCommunication"),
    MUSIC_NOTATION("MusicNotation"),
    OTHER_STUDENT_WORK("OtherStudentWork"),
    REPORT_BASIC("ReportBasic"),
    REPORT_POLICY("ReportPolicy"),
    REPORT_RESEARCH("ReportResearch"),
    REPORT_WORKING_PAPER("ReportWorkingPaper");

    private final String name;

    PublicationType(String name) {
        this.name = name;
    }

    /**
     * Find PublicationType given json representation of enum.
     *
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

    @Override
    public String toString() {
        return name;
    }
}
