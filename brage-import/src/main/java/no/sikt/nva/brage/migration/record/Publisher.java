package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Publisher {

    private String pid;

    @JsonCreator
    public Publisher(@JsonProperty("pid") String pid) {
        this.pid = pid;
    }

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
        Publisher publisher = (Publisher) o;
        return Objects.equals(getPid(), publisher.getPid());
    }
}
