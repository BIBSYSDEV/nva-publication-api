package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class PublishedDate {

    private List<String> brageDates;
    private String nvaDate;

    public PublishedDate() {
    }

    @JsonProperty("brage")
    public List<String> getBrageDates() {
        return brageDates;
    }

    @JacocoGenerated
    public void setBrageDates(List<String> brageDates) {
        this.brageDates = brageDates;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(brageDates, nvaDate);
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
        PublishedDate that = (PublishedDate) o;
        return Objects.equals(brageDates, that.brageDates) && Objects.equals(nvaDate, that.nvaDate);
    }

    @JsonProperty("nva")
    public String getNvaDate() {
        return nvaDate;
    }

    public void setNvaDate(String nvaDate) {
        this.nvaDate = nvaDate;
    }
}
