package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_PK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_SK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.utils.JacocoGenerated;

@JsonTypeName("Resource")
public class ResourceDao implements WithPrimaryKey{

    public static final String PATH_SEPARATOR = "/";
    public static final String  ORGANIZATION= Organization.class.getSimpleName();
    public static  final String PUBLICATION_STATUS=  PublicationStatus.class.getSimpleName();

    @JsonProperty("resource")
    private Resource resource;

    public ResourceDao() {
        resource = new Resource();
    }

    public ResourceDao(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getPrimaryKeyPartitionKey() {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT,
            Resource.TYPE, publisherId(), resource.getOwner());
    }

    @JacocoGenerated
    public void setPrimaryKeyPartitionKey(String key) {
        // do nothing
    }

    @Override
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getPrimaryKeySortKey() {
        return String.format(PRIMARY_KEY_SORT_KEY_FORMAT,
            Resource.TYPE, resource.getIdentifier());
    }

    @JacocoGenerated
    public void setPrimaryKeySortKey(String key) {
        // do nothing
    }

    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME)
    public String getByTypeCustomerStatusPk() {
        String publisherId = publisherId();
        String publicationStatus = extractStatus();
        return String.format(BY_TYPE_CUSTOMER_STATUS_PK_FORMAT,
            ORGANIZATION, publisherId,
            PUBLICATION_STATUS,publicationStatus
        );
    }

    private String extractStatus() {
        return Optional.of(resource.getStatus())
            .map(Enum::toString)
            .map(str -> str.toUpperCase(Locale.ROOT))
            .orElse(PublicationStatus.DRAFT.toString());
    }

    public void setByTypeCustomerStatusPk(String byTypeCustomerStatusPk) {
        // do nothing
    }

    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    public String getByTypeCustomerStatusSk() {
        return String.format(BY_TYPE_CUSTOMER_STATUS_SK_FORMAT, Resource.TYPE, resource.getIdentifier().toString());
    }

    public void setByTypeCustomerStatusSk(String byTypeCustomerStatusSk) {
        // do nothing
    }

    @Override
    public Map<String, AttributeValue> primaryKey() {
        final Map<String, AttributeValue> map = new ConcurrentHashMap<>();
        AttributeValue partKeyValue = new AttributeValue(getPrimaryKeyPartitionKey());
        AttributeValue sortKeyValue = new AttributeValue(getPrimaryKeySortKey());
        map.put(PRIMARY_KEY_PARTITION_KEY_NAME, partKeyValue);
        map.put(PRIMARY_KEY_SORT_KEY_NAME, sortKeyValue);
        return map;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getResource());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceDao)) {
            return false;
        }
        ResourceDao that = (ResourceDao) o;
        return Objects.equals(getResource(), that.getResource());
    }

    private String publisherId() {
        String[] pathParts = resource.getPublisher().getId().getPath().split(PATH_SEPARATOR);
        return pathParts[pathParts.length - 1];
    }
}
