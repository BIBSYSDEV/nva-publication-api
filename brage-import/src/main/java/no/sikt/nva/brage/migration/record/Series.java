package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Series {

    private String id;

    @JacocoGenerated
    public Series(@JsonProperty("id") String id) {
        this.id = id;
    }

    @JacocoGenerated
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JacocoGenerated
    public void setId(String id) {
        this.id = id;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId());
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
        Series series = (Series) o;
        return Objects.equals(getId(), series.getId());
    }
}
