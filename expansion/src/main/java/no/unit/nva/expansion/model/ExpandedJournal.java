package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(ExpandedJournal.TYPE)
public class ExpandedJournal {

    public static final String TYPE = "Journal";
    public URI id;

    public String name;

    @JacocoGenerated
    public ExpandedJournal() {

    }

    public ExpandedJournal(URI id, String name) {
        this.id = id;
        this.name = name;
    }

    @JacocoGenerated
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    public String getName() {
        return name;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
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
        ExpandedJournal that = (ExpandedJournal) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }
}
