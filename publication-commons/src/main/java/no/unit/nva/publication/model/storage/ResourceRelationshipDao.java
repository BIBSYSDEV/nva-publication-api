package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import no.unit.nva.publication.model.business.ResourceRelationship;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;

public record ResourceRelationshipDao(ResourceRelationship resourceRelationship, Instant createdDate)
    implements DatabaseEntryWithData<ResourceRelationship> {

    public static ResourceRelationshipDao from(ResourceRelationship resourceRelationship) {
        return new ResourceRelationshipDao(resourceRelationship, Instant.now());
    }

    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getParentKey() {
        return "ParentResource:%s".formatted(resourceRelationship.parentIdentifier().toString());
    }

    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getChildKey() {
        return "ChildResource:%s".formatted(resourceRelationship.childIdentifier().toString());
    }

    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    public String getPK3() {
        return "Resource:%s".formatted(resourceRelationship.parentIdentifier().toString());
    }

    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    public String getPK0() {
        return "ChildResource:%s".formatted(resourceRelationship.childIdentifier().toString());
    }

    @Override
    public ResourceRelationship getData() {
        return resourceRelationship;
    }
}
