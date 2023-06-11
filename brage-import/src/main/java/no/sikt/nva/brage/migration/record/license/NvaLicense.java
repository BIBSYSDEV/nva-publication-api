package no.sikt.nva.brage.migration.record.license;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class NvaLicense {

    private static final String TYPE = "License";
    private NvaLicenseUri licenseUri;


    public NvaLicense(@JsonProperty("identifier") NvaLicenseUri licenseUri) {
        this.licenseUri = licenseUri;
    }

    public NvaLicenseUri getLicenseUri() {
        return licenseUri;
    }

    @JacocoGenerated
    public void setLicenseUri(NvaLicenseUri identifier) {
        this.licenseUri = identifier;
    }


    @JsonProperty("type")
    public String getType() {
        return TYPE;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(TYPE, licenseUri);
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
        return Objects.equals(licenseUri, nvaLicense.licenseUri);
    }
}
