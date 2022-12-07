package no.sikt.nva.brage.migration.record.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class License {

    @JsonInclude
    private String brageLicense;
    private NvaLicense nvaLicense;

    public License(String brageLisense, NvaLicense nvaLicense) {
        this.brageLicense = brageLisense;
        this.nvaLicense = nvaLicense;
    }

    @JacocoGenerated
    public License() {

    }

    @JacocoGenerated
    public String getBrageLicense() {
        return brageLicense;
    }

    @JacocoGenerated
    public void setBrageLicense(String brageLicense) {
        this.brageLicense = brageLicense;
    }

    @JacocoGenerated
    public NvaLicense getNvaLicense() {
        return nvaLicense;
    }

    @JacocoGenerated
    public void setNvaLicense(NvaLicense nvaLicense) {
        this.nvaLicense = nvaLicense;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(brageLicense, nvaLicense);
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
        License license = (License) o;
        return Objects.equals(brageLicense, license.brageLicense)
               && Objects.equals(nvaLicense, license.nvaLicense);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return brageLicense;
    }
}
