package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.LogEntryDao.KEY_PATTERN;
import static no.unit.nva.publication.model.storage.TicketDao.newPutTransactionItem;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublicationChannelDao.TYPE)
public class PublicationChannelDao extends Dao implements DynamoEntryByIdentifier {

    protected static final String TYPE = "PublicationChannel";
    protected static final String IDENTIFIER = "identifier";
    protected static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    protected static final String DATA = "data";
    private final SortableIdentifier identifier;
    private final SortableIdentifier resourceIdentifier;

    @JsonCreator
    public PublicationChannelDao(@JsonProperty(IDENTIFIER) SortableIdentifier identifier,
                                 @JsonProperty(RESOURCE_IDENTIFIER) SortableIdentifier resourceIdentifier,
                                 @JsonProperty(DATA) PublicationChannel publicationChannel) {
        super(publicationChannel);
        this.identifier = identifier;
        this.resourceIdentifier = resourceIdentifier;
    }

    public static PublicationChannelDao fromPublicationChannel(PublicationChannel publicationChannel) {
        var lowerCaseIdentifier = publicationChannel.getIdentifier().toString().toLowerCase(Locale.ROOT);
        return new PublicationChannelDao(new SortableIdentifier(lowerCaseIdentifier),
                                         publicationChannel.getResourceIdentifier(),
                                         publicationChannel);
    }

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getPrimaryKeyPartitionKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }

    @Override
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getPrimaryKeySortKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, getResourceIdentifier());
    }

    //TODO: Remove JacocoGenerated annotation once method is in use
    @JacocoGenerated
    @Override
    public String indexingType() {
        return TYPE;
    }

    @Override
    public String getByTypeCustomerStatusPartitionKey() {
        return null;
    }

    @Override
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    public String getByTypeCustomerStatusSortKey() {
        return null;
    }

    @JacocoGenerated
    @Override
    public URI getCustomerId() {
        return null;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public Map<String, AttributeValue> toDynamoFormat() {
        return attempt(() -> JsonUtils.dynamoObjectMapper.writeValueAsString(this)).map(Item::fromJSON)
                   .map(ItemUtils::toAttributeValues)
                   .orElseThrow();
    }

    //TODO: Remove JacocoGenerated annotation once method is in use
    @JacocoGenerated
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        return new TransactWriteItemsRequest().withTransactItems(newPutTransactionItem(this));
    }

    @JacocoGenerated
    @Override
    public void updateExistingEntry(AmazonDynamoDB client) {
        throw new UnsupportedOperationException();
    }

    @JacocoGenerated
    @Override
    protected User getOwner() {
        return null;
    }

    public TransactWriteItem toPutNewTransactionItem(String tableName) {
        var put = new Put().withItem(this.toDynamoFormat())
                      .withTableName(tableName)
                      .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
        return new TransactWriteItem().withPut(put);
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    public String getByTypeAndIdentifierPartitionKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, getResourceIdentifier());
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    public String getByTypeAndIdentifierSortKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }
}
