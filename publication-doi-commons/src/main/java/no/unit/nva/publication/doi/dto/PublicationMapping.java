package no.unit.nva.publication.doi.dto;

import java.util.Optional;
import nva.commons.core.JacocoGenerated;

/**
 * {@link PublicationMapping} is a holder object for holding `oldPublication` and `newPublication` (DTO) because a
 * DynamodbEvent (DAO) holds a optional reference to `oldImage` and `newImage` from a database change in DynamoDb.
 */
public class PublicationMapping {

    private Publication oldPublication;
    private Publication newPublication;

    public PublicationMapping(Publication oldPublication, Publication newPublication) {
        this.oldPublication = oldPublication;
        this.newPublication = newPublication;
    }

    protected PublicationMapping(PublicationMapping.Builder builder) {
        this(builder.oldPublication, builder.newPublication);
    }

    public Optional<Publication> getOldPublication() {
        return Optional.ofNullable(oldPublication);
    }

    @JacocoGenerated
    public void setOldPublication(Publication oldPublication) {
        this.oldPublication = oldPublication;
    }

    public Optional<Publication> getNewPublication() {
        return Optional.ofNullable(newPublication);
    }

    @JacocoGenerated
    public void setNewPublication(Publication newPublication) {
        this.newPublication = newPublication;
    }

    public static final class Builder {

        private Publication oldPublication;
        private Publication newPublication;

        private Builder() {
        }

        public static PublicationMapping.Builder newBuilder() {
            return new PublicationMapping.Builder();
        }

        public Builder withOldPublication(Publication oldPublication) {
            this.oldPublication = oldPublication;
            return this;
        }

        public Builder withNewPublication(Publication newPublication) {
            this.newPublication = newPublication;
            return this;
        }

        public PublicationMapping build() {
            return new PublicationMapping(this);
        }
    }

}
