package no.unit.nva.publication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import nva.commons.core.JacocoGenerated;

public class PublicationSummary {

    public static final String MAIN_TITLE = "mainTitle";

    private SortableIdentifier identifier;
    private String mainTitle;
    private String owner;
    private Instant modifiedDate;
    private Instant createdDate;
    private PublicationStatus status;

    private PublicationSummary(Builder builder) {
        setIdentifier(builder.identifier);
        setMainTitle(builder.mainTitle);
        setOwner(builder.owner);
        setModifiedDate(builder.modifiedDate);
        setCreatedDate(builder.createdDate);
        setStatus(builder.status);
    }

    public PublicationSummary() {
    }

    public static PublicationSummary fromPublication(Publication publication) {
        return new PublicationSummary.Builder()
            .withIdentifier(publication.getIdentifier())
            .withOwner(publication.getOwner())
            .withMainTitle(extractMainTitle(publication))
            .withModifiedDate(publication.getModifiedDate())
            .withCreatedDate(publication.getCreatedDate())
            .withStatus(publication.getStatus())
            .build();
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(
            getIdentifier(),
            getMainTitle(),
            getOwner(),
            getModifiedDate(),
            getCreatedDate(),
            getStatus());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublicationSummary that = (PublicationSummary) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getMainTitle(), that.getMainTitle())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && getStatus() == that.getStatus();
    }

    private static String extractMainTitle(Publication publication) {
        return Optional.ofNullable(publication)
            .map(Publication::getEntityDescription)
            .map(EntityDescription::getMainTitle)
            .orElse(null);
    }

    @JsonProperty(value = "entityDescription", access = JsonProperty.Access.WRITE_ONLY)
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void unpackNested(Map<String, Object> entityDescription) {
        this.mainTitle = (String) entityDescription.get(MAIN_TITLE);
    }

    public static final class Builder {

        private SortableIdentifier identifier;
        private String mainTitle;
        private String owner;
        private Instant modifiedDate;
        private Instant createdDate;
        private PublicationStatus status;

        public Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder withStatus(PublicationStatus status) {
            this.status = status;
            return this;
        }

        public PublicationSummary build() {
            return new PublicationSummary(this);
        }
    }
}
