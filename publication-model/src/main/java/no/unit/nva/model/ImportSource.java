package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ImportSource implements JsonSerializable {

    public static final String NO_ARCHIVE = null;
    public static final String SOURCE_FIELD = "source";
    public static final String ARCHIVE_FIELD = "archive";
    private final Source source;
    private final String archive;

    @JsonCreator
    public ImportSource(@JsonProperty(SOURCE_FIELD) Source source, @JsonProperty(ARCHIVE_FIELD) String archive) {
        this.source = source;
        this.archive = archive;
    }

    public static ImportSource fromBrageArchive(String archive) {
        return new ImportSource(Source.BRAGE, archive);
    }

    public static ImportSource fromSource(Source source) {
        return new ImportSource(source, NO_ARCHIVE);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(source, archive);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImportSource that)) {
            return false;
        }
        return source == that.source && Objects.equals(archive, that.archive);
    }

    public Source getSource() {
        return source;
    }

    public String getArchive() {
        return archive;
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    public enum Source {
        BRAGE, CRISTIN, SCOPUS
    }
}
