package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_PK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_SK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.daos.ResourceDao.PATH_SEPARATOR;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.WithIdentifier;
import no.unit.nva.publication.storage.model.WithStatus;
import nva.commons.core.JacocoGenerated;

public abstract class Dao<R extends WithIdentifier & RowLevelSecurity> implements WithPrimaryKey {

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public final String getPrimaryKeyPartitionKey() {
        return formatPrimaryPartitionKey(getCustomerId(), getOwner());
    }

    @Override
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public final String getPrimaryKeySortKey() {
        return String.format(PRIMARY_KEY_SORT_KEY_FORMAT, getType(), getIdentifier());
    }

    public final Map<String, AttributeValue> primaryKey() {
        final Map<String, AttributeValue> map = new ConcurrentHashMap<>();
        AttributeValue partKeyValue = new AttributeValue(getPrimaryKeyPartitionKey());
        AttributeValue sortKeyValue = new AttributeValue(getPrimaryKeySortKey());
        map.put(PRIMARY_KEY_PARTITION_KEY_NAME, partKeyValue);
        map.put(PRIMARY_KEY_SORT_KEY_NAME, sortKeyValue);
        return map;
    }

    @JacocoGenerated
    public final void setPrimaryKeySortKey(String key) {
        // do nothing
    }

    @JacocoGenerated
    public final void setPrimaryKeyPartitionKey(String key) {
        // do nothing
    }

    @JsonProperty("data")
    public abstract R getData();

    public abstract void setData(R data);

    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME)
    public final String getByTypeCustomerStatusPartitionKey() {
        String publisherId = customerIdentifier();
        Optional<String> publicationStatus = extractStatus();

        return publicationStatus
            .map(status -> formatByTypeCustomerStatusIndexPartitionKey(publisherId, status))
            .orElse(null);
    }

    public void setByTypeCustomerStatusPartitionKey(String byTypeCustomerStatusPk) {
        // do nothing
    }

    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    public final String getByTypeCustomerStatusSortKey() {
        return String.format(BY_TYPE_CUSTOMER_STATUS_SK_FORMAT, Resource.TYPE, getData().getIdentifier().toString());
    }

    public final void setByTypeCustomerStatusSortKey(String byTypeCustomerStatusSk) {
        // do nothing
    }

    @JsonIgnore
    public final String getCustomerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }

    protected static String orgUriToOrgIdentifier(URI uri) {
        String[] pathParts = uri.getPath().split(PATH_SEPARATOR);
        return pathParts[pathParts.length - 1];
    }

    protected String formatPrimaryPartitionKey(URI organizationUri, String userIdentifier) {
        String organizationIdentifier = orgUriToOrgIdentifier(organizationUri);
        return formatPrimaryPartitionKey(organizationIdentifier, userIdentifier);
    }

    protected String formatPrimaryPartitionKey(String publisherId, String owner) {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT, getType(), publisherId, owner);
    }

    @JsonIgnore
    protected abstract String getType();

    @JsonIgnore
    protected abstract URI getCustomerId();

    @JsonIgnore
    protected abstract String getOwner();

    @JsonIgnore
    protected abstract String getIdentifier();

    private String formatByTypeCustomerStatusIndexPartitionKey(String publisherId, String status) {
        return String.format(BY_TYPE_CUSTOMER_STATUS_PK_FORMAT,
            getType(),
            publisherId,
            status);
    }

    private Optional<String> extractStatus() {
        return attempt(this::getData)
            .map(data -> (WithStatus) data)
            .map(WithStatus::getStatus)
            .toOptional();
    }

    private String customerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }
}
