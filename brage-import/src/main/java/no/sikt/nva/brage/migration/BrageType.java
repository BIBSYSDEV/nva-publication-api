package no.sikt.nva.brage.migration;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public enum BrageType {

    BOOK("Book"),
    CHAPTER("Chapter"),
    DATASET("Dataset"),
    JOURNAL_ARTICLE("Journal article"),
    OTHERS("Others"),
    REPORT("Report"),
    RESEARCH_REPORT("Research report"),
    BACHELOR_THESIS("Bachelor thesis"),
    MASTER_THESIS("Master thesis"),
    DOCTORAL_THESIS("Doctoral thesis"),
    WORKING_PAPER("Working paper"),
    STUDENT_PAPER("Student paper"),
    STUDENT_PAPER_OTHERS("Student paper, others"),
    DESIGN_PRODUCT("Design product"),
    CHRONICLE("Chronicle"),
    SOFTWARE("Software"),
    LECTURE("Lecture"),
    RECORDING_MUSICAL("Recording, musical"),
    RECORDING_ORAL("Recording, oral"),
    PLAN_OR_BLUEPRINT("Plan or blueprint"),
    MAP("Map"),
    PEER_REVIEWED("Peer reviewed");

    private final String value;

    BrageType(String type) {
        this.value = type;
    }

    public static BrageType fromValue(String v) {
        for (BrageType c : BrageType.values()) {
            if (c.getValue().equalsIgnoreCase(v)) {
                return c;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return value;
    }
}
