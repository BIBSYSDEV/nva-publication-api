package no.unit.nva.publication.model.business.importcandidate;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

public class ImportStatus implements JsonSerializable {

    private final CandidateStatus candidateStatus;

    private final Username setBy;

    private final Instant modifiedDate;

    private final URI nvaPublicationId;

    private final String comment;

    @ConstructorProperties({"candidateStatus", "modifiedDate", "setBy", "nvaPublicationId", "comment"})
    public ImportStatus(CandidateStatus candidateStatus, Instant modifiedDate, Username setBy, URI nvaPublicationId,
                        String comment) {
        this.candidateStatus = candidateStatus;
        this.modifiedDate = modifiedDate;
        this.setBy = setBy;
        this.nvaPublicationId = nvaPublicationId;
        this.comment = comment;
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

    public CandidateStatus getCandidateStatus() {
        return candidateStatus;
    }

    public Username getSetBy() {
        return setBy;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public URI getNvaPublicationId() {
        return nvaPublicationId;
    }

    public String getComment() {
        return comment;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getCandidateStatus(), getSetBy(), getModifiedDate(), getNvaPublicationId(), getComment());
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
        return getCandidateStatus() == that.getCandidateStatus()
               && Objects.equals(getSetBy(), that.getSetBy())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getNvaPublicationId(), that.getNvaPublicationId())
               && Objects.equals(getComment(), that.getComment());
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
