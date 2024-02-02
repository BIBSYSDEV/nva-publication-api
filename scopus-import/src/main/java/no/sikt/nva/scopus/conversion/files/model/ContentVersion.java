package no.sikt.nva.scopus.conversion.files.model;

import nva.commons.core.JacocoGenerated;

public enum ContentVersion {

    VOR("vor"), AM("am"), TDM("tdm"), UNSPECIFIED("unspecified");

    private final String value;
    ContentVersion(String type) {
        this.value = type;
    }

    @JacocoGenerated
    public String getValue() {
        return value;
    }
}
