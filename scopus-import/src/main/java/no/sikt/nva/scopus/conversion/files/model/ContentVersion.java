package no.sikt.nva.scopus.conversion.files.model;

public enum ContentVersion {

    VOR("vor"), AM("am"), TDM("tdm"), UNSPECIFIED("unspecified");

    private final String value;
    ContentVersion(String type) {
        this.value = type;
    }

    public String getValue() {
        return value;
    }
}
