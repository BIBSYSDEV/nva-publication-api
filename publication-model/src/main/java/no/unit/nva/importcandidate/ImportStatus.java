package no.unit.nva.importcandidate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.beans.ConstructorProperties;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record ImportStatus(CandidateStatus candidateStatus, Instant modifiedDate, Username setBy, URI nvaPublicationId,
                           String comment) implements JsonSerializable {

    @ConstructorProperties({"candidateStatus", "modifiedDate", "setBy", "nvaPublicationId", "comment"})
    public ImportStatus {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return builder().withNvaPublicationId(nvaPublicationId)
                   .withComment(comment)
                   .withSetBy(setBy)
                   .withModifiedDate(modifiedDate)
                   .withCandidateStatus(candidateStatus);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(candidateStatus(), setBy(), modifiedDate(), nvaPublicationId(), comment());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImportStatus that)) {
            return false;
        }
        return candidateStatus() == that.candidateStatus()
               && Objects.equals(setBy(), that.setBy())
               && Objects.equals(modifiedDate(), that.modifiedDate())
               && Objects.equals(nvaPublicationId(), that.nvaPublicationId())
               && Objects.equals(comment(), that.comment());
    }

    public static final class Builder {

        private CandidateStatus candidateStatus;
        private Username setBy;
        private Instant modifiedDate;
        private URI nvaPublicationId;
        private String comment;

        private Builder() {
        }

        public Builder withCandidateStatus(CandidateStatus candidateStatus) {
            this.candidateStatus = candidateStatus;
            return this;
        }

        public Builder withSetBy(Username setBy) {
            this.setBy = setBy;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder withNvaPublicationId(URI nvaPublicationId) {
            this.nvaPublicationId = nvaPublicationId;
            return this;
        }

        public Builder withComment(String comment) {
            this.comment = comment;
            return this;
        }

        public ImportStatus build() {
            return new ImportStatus(candidateStatus, modifiedDate, setBy, nvaPublicationId, comment);
        }
    }
}
