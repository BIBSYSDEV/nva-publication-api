package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class DoiRequest implements JsonSerializable {

    @JsonProperty("publicationId")
    private final URI publicationId;
    @JsonProperty("customerId")
    private final URI customerId;
    @JsonProperty("doi")
    private final URI doi;

    public DoiRequest(@JsonProperty("publicationId") URI publicationId,
                      @JsonProperty("customerId") URI customerId,
                      @JsonProperty("doi") URI doi) {
        this.doi = doi;
        this.customerId = customerId;
        this.publicationId = publicationId;
    }


    @JacocoGenerated
    public URI getPublicationId() {
        return publicationId;
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public URI getDoi() {
        return doi;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublicationId(), getCustomerId(), getDoi());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiRequest)) {
            return false;
        }
        DoiRequest that = (DoiRequest) o;
        return Objects.equals(getPublicationId(), that.getPublicationId()) && Objects.equals(
            getCustomerId(), that.getCustomerId()) && Objects.equals(getDoi(), that.getDoi());
    }

    public static class Builder {

        private URI publicationId;
        private URI customerId;
        private URI doi;

        public Builder withPublicationId(URI publicationId) {
            this.publicationId = publicationId;
            return this;
        }

        public Builder withCustomerId(URI customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public DoiRequest build() throws BadRequestException {
            return new DoiRequest(publicationId, customerId, doi);
        }
    }
}
