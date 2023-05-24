package no.sikt.nva.brage.migration;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public enum BrageType {

    BOOK("Book"),
    BOOK_OF_ABSTRACTS("Book of abstracts"),
    CHAPTER("Chapter"),
    DATASET("Dataset"),
    DATA_SET("Data set"),
    JOURNAL_ARTICLE("Journal article"),
    JOURNAL_ISSUE("Journal issue"),
    OTHERS("Others"),
    OTHER("Other"),
    REPORT("Report"),
    RESEARCH_REPORT("Research report"),
    BACHELOR_THESIS("Bachelor thesis"),
    MASTER_THESIS("Master thesis"),
    SPECIAL_THESIS("Spesialavhandling"),
    DOCTORAL_THESIS("Doctoral thesis"),
    WORKING_PAPER("Working paper"),
    STUDENT_PAPER("Student paper"),
    STUDENT_PAPER_OTHERS("Student paper, others"),
    STUDENT_THESIS_OTHER("Other student thesis"),
    DESIGN_PRODUCT("Design product"),
    CHRONICLE("Chronicle"),
    SOFTWARE("Software"),
    LECTURE("Lecture"),
    RECORDING_MUSICAL("Recording, musical"),
    RECORDING_ORAL("Recording, oral"),
    PLAN_OR_BLUEPRINT("Plan or blueprint"),
    MAP("Map"),
    NOTES("Notat"),
    POSTER("Poster"),
    PRESENTATION("Presentasjon"),
    CONFERENCE_OBJECT("Conference object"),
    CONFERENCE_POSTER("Conference poster"),
    CONFERENCE_LECTURE("Conference lecture"),
    PRESENTATION_OTHER("Other presentation"),
    INTERVIEW("Interview"),
    PEER_REVIEWED("Peer reviewed");
    private final String value;

    BrageType(String type) {
        this.value = type;
    }

    public String getValue() {
        return value;
    }
}
