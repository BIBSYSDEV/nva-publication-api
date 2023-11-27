package no.unit.nva.publication;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSerialize
public record PublicationDetail(SortableIdentifier identifier,
                                URI duplicateOf,
                                EntityDescription entityDescription) implements JsonSerializable {

    public static PublicationDetail fromPublication(Publication publication) {
        return PublicationDetail.builder()
                   .withIdentifier(publication.getIdentifier())
                   .withDuplicateOf(publication.getDuplicateOf())
                   .withEntityDescription(publication.getEntityDescription())
                   .build();
    }

    public String toString() {
       return this.toJsonString();
    }

    private static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private SortableIdentifier identifier;
        private URI duplicateOf;
        private EntityDescription entityDescription;

        private Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withDuplicateOf(URI duplicateOf) {
            this.duplicateOf = duplicateOf;
            return this;
        }

        public Builder withEntityDescription(EntityDescription entityDescription) {
            this.entityDescription = entityDescription;
            return this;
        }

        public PublicationDetail build() {
            return new PublicationDetail(identifier, duplicateOf, entityDescription);
        }
    }
}
