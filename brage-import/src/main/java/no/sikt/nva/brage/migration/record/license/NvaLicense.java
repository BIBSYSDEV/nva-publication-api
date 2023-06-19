package no.sikt.nva.brage.migration.record.license;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class NvaLicense {

    private static final String TYPE = "License";
    private URI license;

    public NvaLicense(@JsonProperty("license") URI license) {
        this.license = license;
    }

    public URI getLicense() {
        return license;
    }

    @JacocoGenerated
    public void setLicense(URI license) {
        this.license = license;
    }

    @JsonProperty("type")
    public String getType() {
        return TYPE;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(TYPE, license);
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
        return Objects.equals(license, nvaLicense.license);
    }
}
