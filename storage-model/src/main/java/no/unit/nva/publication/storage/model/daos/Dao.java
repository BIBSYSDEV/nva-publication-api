package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_PK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_SK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_FORMAT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.WithIdentifier;
import no.unit.nva.publication.storage.model.WithStatus;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")

@JsonSubTypes({
    @JsonSubTypes.Type(name = ResourceDao.TYPE, value = ResourceDao.class),
    @JsonSubTypes.Type(name = DoiRequestDao.TYPE, value = DoiRequestDao.class),
    @JsonSubTypes.Type(name = MessageDao.TYPE, value = MessageDao.class),
    @JsonSubTypes.Type(name = PublicationRequestDao.TYPE, value = PublicationRequestDao.class),
})
public abstract class Dao<R extends WithIdentifier & RowLevelSecurity & DataEntry>
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

    public static String scanFilterExpression() {
        return "begins_with (#PK, :Resource) or "
               + "begins_with(#PK, :DoiRequest) or "
               + "begins_with(#PK, :Message) or "
               + "begins_with(#PK, :PublicationRequest)";
    }

    // replaces the hash values in the filter expression with the actual key name
    public static Map<String, String> scanFilterExpressionAttributeNames() {
        return Map.of("#PK", DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME);
    }

    // replaces the colon values in the filter expression with the actual value
    public static Map<String, AttributeValue> scanFilterExpressionAttributeValues() {
        return Map.of(":Resource", new AttributeValue(ResourceDao.TYPE + KEY_FIELDS_DELIMITER),
                      ":DoiRequest", new AttributeValue(DoiRequestDao.TYPE + KEY_FIELDS_DELIMITER),
                      ":Message", new AttributeValue(MessageDao.TYPE + KEY_FIELDS_DELIMITER),
                ":PublicationRequest", new AttributeValue(PublicationRequestDao.TYPE + KEY_FIELDS_DELIMITER)
        );
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
}
