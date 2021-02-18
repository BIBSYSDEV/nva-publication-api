package no.unit.nva.publication.storage.model.daos;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_PK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_SK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_FORMAT;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.WithIdentifier;
import no.unit.nva.publication.storage.model.WithStatus;
import no.unit.nva.publication.storage.model.exceptions.EmptyValueMapException;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")

@JsonSubTypes({
    @JsonSubTypes.Type(name = "Resource", value = ResourceDao.class),
    @JsonSubTypes.Type(name = "DoiRequest", value = DoiRequestDao.class),
    @JsonSubTypes.Type(name = "Message", value = MessageDao.class),
})
public abstract class Dao<R extends WithIdentifier & RowLevelSecurity & ResourceUpdate>
    implements DynamoEntry,
               WithPrimaryKey,
               WithByTypeCustomerStatusIndex,
               WithIdentifier {

    public static final String URI_PATH_SEPARATOR = "/";
    public static final String CONTAINED_DATA_FIELD_NAME = "data";
    public static final String UNSUPORTED_SET_IDENTIFIER_ERROR =
        "Daos cannot set their identifier. They get it from their contained data";

    public static String orgUriToOrgIdentifier(URI uri) {
        String[] pathParts = uri.getPath().split(URI_PATH_SEPARATOR);
        return pathParts[pathParts.length - 1];
    }

    @Override
    public final String getPrimaryKeyPartitionKey() {
        return formatPrimaryPartitionKey(getCustomerId(), getOwner());
    }

    @Override
    @JacocoGenerated
    public final void setPrimaryKeyPartitionKey(String key) {
        // do nothing
    }

    @Override
    @JacocoGenerated
    public final String getPrimaryKeySortKey() {
        return String.format(PRIMARY_KEY_SORT_KEY_FORMAT, getType(), getIdentifier());
    }

    @Override
    @JacocoGenerated
    public final void setPrimaryKeySortKey(String key) {
        // do nothing
    }

    @JsonProperty(CONTAINED_DATA_FIELD_NAME)
    public abstract R getData();

    public abstract void setData(R data);

    @Override
    public final String getByTypeCustomerStatusPartitionKey() {
        String publisherId = customerIdentifier();
        Optional<String> publicationStatus = extractStatus();

        return publicationStatus
            .map(status -> formatByTypeCustomerStatusIndexPartitionKey(publisherId, status))
            .orElse(null);
    }

    @Override
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    public final String getByTypeCustomerStatusSortKey() {
        //Codacy complains that identifier is already a String
        SortableIdentifier identifier = getData().getIdentifier();
        return String.format(BY_TYPE_CUSTOMER_STATUS_SK_FORMAT, this.getType(), identifier.toString());
    }

    @JsonIgnore
    public final String getCustomerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }

    @JsonIgnore
    public abstract String getType();

    @JsonIgnore
    public abstract URI getCustomerId();

    @JsonIgnore
    @Override
    public abstract SortableIdentifier getIdentifier();

    @Override
    @JacocoGenerated
    public final void setIdentifier(SortableIdentifier identifier) {
        throw new UnsupportedOperationException(UNSUPORTED_SET_IDENTIFIER_ERROR);
    }

    protected String formatPrimaryPartitionKey(URI organizationUri, String userIdentifier) {
        String organizationIdentifier = orgUriToOrgIdentifier(organizationUri);
        return formatPrimaryPartitionKey(organizationIdentifier, userIdentifier);
    }

    protected String formatPrimaryPartitionKey(String publisherId, String owner) {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT, getType(), publisherId, owner);
    }

    @JsonIgnore
    protected abstract String getOwner();

    private String formatByTypeCustomerStatusIndexPartitionKey(String publisherId, String status) {
        return String.format(BY_TYPE_CUSTOMER_STATUS_PK_FORMAT,
            getType(),
            publisherId,
            status);
    }
    
    private Optional<String> extractStatus() {
        return attempt(this::getData)
                   .map(data -> (WithStatus) data)
                   .map(WithStatus::getStatusString)
                   .toOptional();
    }
    
    private String customerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }

    public static <T> T parseAttributeValuesMap(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        if (mapIsNotEmpty(valuesMap)) {
            Item item = ItemUtils.toItem(valuesMap);
            return attempt(() -> objectMapper.readValue(item.toJSON(), daoClass)).orElseThrow();
        } else {
            throw new EmptyValueMapException();
        }
    }

    private static boolean mapIsNotEmpty(Map<String, AttributeValue> valuesMap) {
        return nonNull(valuesMap) && !valuesMap.isEmpty();
    }

    public Map<String, AttributeValue> toDynamoFormat() {
        Item item = attempt(() -> Item.fromJSON(objectMapper.writeValueAsString(this))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }
}
