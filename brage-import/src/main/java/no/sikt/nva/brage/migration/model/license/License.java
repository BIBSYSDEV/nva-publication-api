package no.sikt.nva.brage.migration.model.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class License {

    @JsonInclude
    private String brageLicense;
    private NvaLicense nvaLicense;

    public License(String brageLisense, NvaLicense nvaLicense) {
        this.brageLicense = brageLisense;
        this.nvaLicense = nvaLicense;
    }

    public License() {

    }

    public String getBrageLicense() {
        return brageLicense;
    }

    public void setBrageLicense(String brageLicense) {
        this.brageLicense = brageLicense;
    }

    public NvaLicense getNvaLicense() {
        return nvaLicense;
    }

    public void setNvaLicense(NvaLicense nvaLicense) {
        this.nvaLicense = nvaLicense;
    }

    @Override
    public int hashCode() {
        return Objects.hash(brageLicense, nvaLicense);
    }



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

    @Override
    public String toString() {
        return brageLicense;
    }

}
