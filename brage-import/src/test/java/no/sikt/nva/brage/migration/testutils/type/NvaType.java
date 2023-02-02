package no.sikt.nva.brage.migration.testutils.type;

public enum NvaType {
    BOOK("Book"),
    CHAPTER("Chapter"),
    DATASET("DataSet"),
    JOURNAL_ARTICLE("JournalArticle"),
    REPORT("Other report"),
    BACHELOR_THESIS("DegreeBachelor"),
    MASTER_THESIS("DegreeMaster"),
    DOCTORAL_THESIS("Doctoral thesis"),
    WORKING_PAPER("ReportWorkingPaper"),
    STUDENT_PAPER("OtherStudentWork"),
    STUDENT_PAPER_OTHERS("Other student thesis"),
    RESEARCH_REPORT("ReportResearch"),
    DESIGN_PRODUCT("Design"),
    CHRONICLE("Feature article"),
    SOFTWARE("Programvare"),
    LECTURE("Lecture"),
    RECORDING_MUSICAL("Music"),
    RECORDING_ORAL("Lydopptak, verbalt"),
    PLAN_OR_BLUEPRINT("Architecture"),
    MAP("Map"),
    CONFERENCE_POSTER("ConferencePoster"),
    SCIENTIFIC_MONOGRAPH("Vitenskapelig monografi"),
    SCIENTIFIC_CHAPTER("Vitenskapelig kapittel"),
    SCIENTIFIC_ARTICLE("Vitenskapelig artikkel");

    private final String value;

    NvaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
