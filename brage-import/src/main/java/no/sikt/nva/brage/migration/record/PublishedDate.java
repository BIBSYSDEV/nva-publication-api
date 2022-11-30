package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class PublishedDate {

    private List<String> brageDates;
    private String nvaDate;

    @JsonCreator
    public PublishedDate(@JsonProperty("brage") List<String> brageDates,
                         @JsonProperty("nva") String nvaDate) {
        this.brageDates = brageDates;
        this.nvaDate = nvaDate;
    }

    public List<String> getBrageDates() {
        return brageDates;
    }

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

    public String getNvaDate() {
        return nvaDate;
    }

    public void setNvaDate(String nvaDate) {
        this.nvaDate = nvaDate;
    }
}
