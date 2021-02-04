package no.unit.nva.publication.doi.update.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JsonSerializable;

public class DoiUpdateDto implements JsonSerializable {

    private final URI doi;
    private final SortableIdentifier publicationIdentifier;
    private final Instant modifiedDate;

    /**
     * Constructor for DoiUpdateDto.
     *
     * @param doi                   doi
     * @param publicationIdentifier publicationId
     * @param modifiedDate          modifiedDate
     */
    @JsonCreator
    public DoiUpdateDto(@JsonProperty("doi") URI doi,
                        @JsonProperty("publicationId") SortableIdentifier publicationIdentifier,
                        @JsonProperty("modifiedDate") Instant modifiedDate) {
        this.doi = doi;
        this.publicationIdentifier = publicationIdentifier;
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

    public SortableIdentifier getPublicationIdentifier() {
        return publicationIdentifier;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public boolean hasAllRequiredValuesSet() {
        return getPublicationIdentifier() != null && getModifiedDate() != null;
    }

    public static class Builder {

        private URI doi;
        private SortableIdentifier publicationIdentifier;
        private Instant modifiedDate;

        public Builder() {
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Builder withPublicationId(SortableIdentifier publicationIdentifier) {
            this.publicationIdentifier = publicationIdentifier;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public DoiUpdateDto build() {
            return new DoiUpdateDto(doi, publicationIdentifier, modifiedDate);
        }
    }
}
