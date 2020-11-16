package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import no.unit.nva.events.models.JsonSerializable;
import nva.commons.utils.JacocoGenerated;

public class DoiUpdateDto implements JsonSerializable {

    private final String doi; // TODO: should be URI
    private final URI publicationId;
    private final Instant modifiedDate;

    /**
     * Constructor for DoiUpdateDto.
     *
     * @param doi   doi
     * @param publicationId publicationId
     * @param modifiedDate  modifiedDate
     */
    @JsonCreator
    public DoiUpdateDto(@JsonProperty("doi") String doi,
                        @JsonProperty("publicationId") URI publicationId,
                        @JsonProperty("modifiedDate") Instant modifiedDate) {
        this.doi = doi;
        this.publicationId = publicationId;
        this.modifiedDate = modifiedDate;
    }

    public Optional<String> getDoi() {
        return Optional.ofNullable(doi);
    }

    public URI getPublicationId() {
        return publicationId;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public boolean hasAllValuesSet() {
        return getDoi().isPresent() && getPublicationId() != null && getModifiedDate() != null;
    }

    // Currently using test resources as input and not the builder.
    @JacocoGenerated
    public static class Builder {

        private String doi;
        private URI publicationId;
        private Instant modifiedDate;

        public Builder() {
        }

        public Builder withDoi(String doi) {
            this.doi = doi;
            return this;
        }

        public Builder withPublicationId(URI publicationId) {
            this.publicationId = publicationId;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public DoiUpdateDto build() {
            return new DoiUpdateDto(doi, publicationId, modifiedDate);
        }
    }
}
