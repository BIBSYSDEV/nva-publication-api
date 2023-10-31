package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Journal {

    private String pid;

    @JacocoGenerated
    @JsonCreator
    public Journal(@JsonProperty("pid") String pid) {
        this.pid = pid;
    }

    @JacocoGenerated
    @JsonProperty("pid")
    public String getPid() {
        return pid;
    }

    @JacocoGenerated
    public void setPid(String pid) {
        this.pid = pid;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPid());
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
        Journal journal = (Journal) o;
        return Objects.equals(getPid(), journal.getPid());
    }
}
