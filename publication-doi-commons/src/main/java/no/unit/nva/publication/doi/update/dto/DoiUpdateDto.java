package no.unit.nva.publication.doi.update.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import nva.commons.utils.JacocoGenerated;

public class DoiUpdateDto implements JsonSerializable {

    private final URI doi;
    private final URI publicationId;
    private final Instant modifiedDate;

    /**
     * Constructor for DoiUpdateDto.
     *
     * @param doi           doi
     * @param publicationId publicationId
     * @param modifiedDate  modifiedDate
     */
    @JsonCreator
    public DoiUpdateDto(@JsonProperty("doi") URI doi,
                        @JsonProperty("publicationId") URI publicationId,
                        @JsonProperty("modifiedDate") Instant modifiedDate) {
        this.doi = doi;
        this.publicationId = publicationId;
        this.modifiedDate = modifiedDate;
    }

    /**
     * Retrieve DOI from DOI update.
     *
     * @return DOI is present if it should be updated, or absent if it is to be removed.
     */
    public Optional<URI> getDoi() {
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

        private URI doi;
        private URI publicationId;
        private Instant modifiedDate;

        public Builder() {
        }

        public Builder withDoi(URI doi) {
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
