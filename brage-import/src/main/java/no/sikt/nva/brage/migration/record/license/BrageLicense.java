package no.sikt.nva.brage.migration.record.license;

import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public enum BrageLicense {
    CC_BY("by"),
    CC_BY_NC("by-nc"),
    CC_BY_NC_ND("by-nc-nd"),
    CC_BY_NC_SA("by-nc-sa"),
    CC_BY_ND("by-nd"),
    CC_BY_SA("by-sa");

    private final String value;

    BrageLicense(String value) {
        this.value = value;
    }

    public static BrageLicense fromValue(String value) {
        for (BrageLicense license : BrageLicense.values()) {
            if (license.getValue().equalsIgnoreCase(value)) {
                return license;
            }
        }
        return null;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
