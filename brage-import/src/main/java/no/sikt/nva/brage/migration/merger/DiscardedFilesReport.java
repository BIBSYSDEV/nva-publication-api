package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.associatedartifacts.file.File;
import nva.commons.core.JacocoGenerated;

public class DiscardedFilesReport implements JsonSerializable {

    private final List<File> discardedFromBrageRecord;
    private final List<File> discardedFromUpdatedRecord;

    @JsonCreator
    public DiscardedFilesReport(
        @JsonProperty("discardedFromBrageRecord") List<File> discardedFromBrageRecord,
        @JsonProperty("discardedFromUpdatedRecord") List<File> discardedFromUpdatedRecord) {
        this.discardedFromBrageRecord = discardedFromBrageRecord;
        this.discardedFromUpdatedRecord = discardedFromUpdatedRecord;
    }

    @JacocoGenerated
    public List<File> getDiscardedFromBrageRecord() {
        return nonNull(discardedFromBrageRecord) ? discardedFromBrageRecord : List.of();
    }

    @JacocoGenerated
    public List<File> getDiscardedFromUpdatedRecord() {
        return nonNull(discardedFromUpdatedRecord) ? discardedFromUpdatedRecord : List.of();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(discardedFromBrageRecord, discardedFromUpdatedRecord);
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
        DiscardedFilesReport that = (DiscardedFilesReport) o;
        return Objects.equals(discardedFromBrageRecord, that.discardedFromBrageRecord)
               && Objects.equals(discardedFromUpdatedRecord, that.discardedFromUpdatedRecord);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }
}
