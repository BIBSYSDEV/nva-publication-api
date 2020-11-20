package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiRequest extends Validatable {

    private final static Logger logger = LoggerFactory.getLogger(DoiRequest.class);
    private final DoiRequestStatus status;
    private final Instant modifiedDate;

    /**
     * Constructor for basic deserialization of DoiRequest.
     *
     * @param status       doi request status
     * @param modifiedDate modified date of doi request
     */
    public DoiRequest(
        @JsonProperty("status") DoiRequestStatus status,
        @JsonProperty("modifiedDate") Instant modifiedDate) {
        super();
        this.status = status;
        this.modifiedDate = modifiedDate;
        requireFieldIsNotNull(status, "DoiRequest.status");
        requireFieldIsNotNull(modifiedDate, "DoiRequest.modifiedDate");
    }

    public DoiRequestStatus getStatus() {
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

    @Override
    protected Logger logger() {
        return logger;
    }

    public static final class Builder {

        private DoiRequestStatus status;
        private Instant modifiedDate;

        public Builder() {
        }

        public Builder withStatus(DoiRequestStatus status) {
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
