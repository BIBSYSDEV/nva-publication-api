package no.sikt.nva.scopus.conversion.files.model;

import static java.util.Arrays.stream;

public enum ContentVersion {

    VOR("vor"), AM("am"), TDM("tdm"), UNSPECIFIED("unspecified");
    private final String value;

    ContentVersion(String type) {
        this.value = type;
    }

    public static ContentVersion fromValue(String value) {
        return stream(values()).filter(contentVersion -> contentVersion.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElse(null);
    }

    public String getValue() {
        return value;
    }
}
