package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.JacocoGenerated;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PublicationSummary {

    public static final String MAIN_TITLE = "mainTitle";

    private UUID identifier;
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

    public UUID getIdentifier() {
        return identifier;
    }

    public void setIdentifier(UUID identifier) {
        this.identifier = identifier;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    @JsonProperty(value = "entityDescription", access = JsonProperty.Access.WRITE_ONLY)
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void unpackNested(Map<String,Object> entityDescription) {
        this.mainTitle = (String)entityDescription.get(MAIN_TITLE);
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

    public static final class Builder {
        private UUID identifier;
        private String mainTitle;
        private String owner;
        private Instant modifiedDate;
        private Instant createdDate;
        private PublicationStatus status;

        public Builder() {
        }

        public Builder withIdentifier(UUID identifier) {
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
