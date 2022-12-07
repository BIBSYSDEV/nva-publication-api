package no.sikt.nva.brage.migration.record.license;

import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

public enum NvaLicenseIdentifier {
    CC_BY("CC BY"),
    CC_BY_NC("CC BY-NC"),
    CC_BY_NC_ND("CC BY-NC-ND"),
    CC_BY_NC_SA("CC BY-NC-SA"),
    CC_BY_ND("CC BY-ND"),
    CC_BY_SA("CC BY-SA"),

    DEFAULT_LICENSE("Rights Reserved");
    private final String value;

    NvaLicenseIdentifier(String value) {
        this.value = value;
    }

    @JacocoGenerated
    @JsonValue
    public String getValue() {
        return value;
    }
}
