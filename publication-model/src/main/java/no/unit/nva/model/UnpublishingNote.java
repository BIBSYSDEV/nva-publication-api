package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeName("UnpublishingNote")
public class UnpublishingNote extends PublicationNoteBase {

    private Username createdBy;
    private Instant createdDate;

    @JsonCreator
    public UnpublishingNote(@JsonProperty("note") String note, @JsonProperty("createdBy") Username createdBy,
                            @JsonProperty("createdDate") Instant createdDate) {
        super(note);
        this.createdBy = createdBy;
        this.createdDate = createdDate;
    }

    public Username getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Username createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), createdBy, createdDate);
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
        if (!super.equals(o)) {
            return false;
        }
        var that = (UnpublishingNote) o;
        return Objects.equals(createdBy, that.createdBy)
               && Objects.equals(createdDate, that.createdDate);
    }
}
