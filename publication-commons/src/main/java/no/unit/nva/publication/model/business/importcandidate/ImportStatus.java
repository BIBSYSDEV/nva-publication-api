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
    public ImportStatus(CandidateStatus candidateStatus, Instant modifiedDate, Username setBy,
                        URI nvaPublicationId, String comment) {
        this.candidateStatus = candidateStatus;
        this.modifiedDate = modifiedDate;
        this.setBy = setBy;
        this.nvaPublicationId = nvaPublicationId;
        this.comment = comment;
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
        if (!(o instanceof ImportStatus)) {
            return false;
        }
        ImportStatus that = (ImportStatus) o;
        return getCandidateStatus() == that.getCandidateStatus()
               && Objects.equals(getSetBy(), that.getSetBy())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getNvaPublicationId(), that.getNvaPublicationId())
               && Objects.equals(getComment(), that.getComment());
    }
}
