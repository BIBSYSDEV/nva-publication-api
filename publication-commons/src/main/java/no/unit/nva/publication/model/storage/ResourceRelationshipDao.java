package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.publication.model.business.ResourceRelationship;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;

@JsonTypeName(ResourceRelationshipDao.TYPE)
@JsonTypeInfo(use = Id.NAME, property = "type")
public record ResourceRelationshipDao(@JsonProperty("data") ResourceRelationship resourceRelationship,
                                      Instant createdDate)
    implements DatabaseEntryWithData<ResourceRelationship> {

    public static final String TYPE = "ResourceRelation";

    public static ResourceRelationshipDao from(ResourceRelationship resourceRelationship) {
        return new ResourceRelationshipDao(resourceRelationship, Instant.now());
    }

    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getChildKey() {
        return "ChildResource:%s".formatted(resourceRelationship.childIdentifier().toString());
    }

    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getParentKey() {
        return "ParentResource:%s".formatted(resourceRelationship.parentIdentifier().toString());
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

    @JsonIgnore
    public Map<String, AttributeValue> getPrimaryKey() {
        return Map.of(
            PRIMARY_KEY_PARTITION_KEY_NAME, new AttributeValue(getChildKey()),
            PRIMARY_KEY_SORT_KEY_NAME, new AttributeValue(getParentKey())
        );
    }
}
