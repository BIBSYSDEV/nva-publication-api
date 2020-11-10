package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

public class DoiRequest {

    private final String status;
    private final Instant modifiedDate;

    /**
     * Constructor for basic deserialization of DoiRequest.
     *
     * @param status        doi request status
     * @param modifiedDate  modified date of doi request
     */
    public DoiRequest(
        @JsonProperty("status") String status,
        @JsonProperty("modifiedDate") Instant modifiedDate) {
        this.status = status;
        this.modifiedDate = modifiedDate;
    }

    public String getStatus() {
        return status;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
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
        DoiRequest that = (DoiRequest) o;
        return Objects.equals(getStatus(), that.getStatus())
            && Objects.equals(getModifiedDate(), that.getModifiedDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getStatus(), getModifiedDate());
    }

    public static final class Builder {

        private String status;
        private Instant modifiedDate;

        public Builder() {
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public DoiRequest build() {
            return new DoiRequest(status, modifiedDate);
        }
    }
}
