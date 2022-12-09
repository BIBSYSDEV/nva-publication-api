package no.sikt.nva.brage.migration.record.license;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class NvaLicense {

    private static final String TYPE = "License";
    private NvaLicenseIdentifier identifier;

    private final Map<String, String> labels;

    public NvaLicense(@JsonProperty("identifier") NvaLicenseIdentifier identifier,
                      @JsonProperty("labels") Map<String, String> labels) {
        this.identifier = identifier;
        this.labels = labels;
    }

    public NvaLicenseIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    public void setIdentifier(NvaLicenseIdentifier identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    @JsonProperty("labels")
    public Map<String, String> getLabels() {
        return labels;
    }

    @JsonProperty("type")
    public String getType() {
        return TYPE;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(TYPE, identifier, labels);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NvaLicense nvaLicense = (NvaLicense) o;
        return Objects.equals(identifier, nvaLicense.identifier)
               && Objects.equals(labels, nvaLicense.labels);
    }
}
