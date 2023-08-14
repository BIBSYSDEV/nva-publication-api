package no.sikt.nva.brage.migration;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public enum NvaType {
    BOOK("Book"),
    CHAPTER("Chapter"),
    DATASET("DataSet"),
    JOURNAL_ARTICLE("JournalArticle"),
    PROFESSIONAL_ARTICLE("Professional article"),
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
    INTERVIEW("Interview"),
    CONFERENCE_POSTER("ConferencePoster"),
    PRESENTATION_OTHER("Other presentation"),
    ANTHOLOGY("Anthology"),
    PERFORMING_ARTS("Performing arts"),
    READER_OPINION("Reader opinion"),
    VISUAL_ARTS("Visual arts"),
    SCIENTIFIC_MONOGRAPH("Vitenskapelig monografi"),
    SCIENTIFIC_CHAPTER("Vitenskapelig kapittel"),
    SCIENTIFIC_ARTICLE("Vitenskapelig artikkel"),
    CONFERENCE_REPORT("ConferenceReport");

    private final String value;

    NvaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
