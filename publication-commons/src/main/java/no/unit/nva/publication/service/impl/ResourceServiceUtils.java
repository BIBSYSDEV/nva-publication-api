package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.service.impl.exceptions.EmptyValueMapException;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;

public final class ResourceServiceUtils {

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Could not find resource";
    public static final String PARTITION_KEY_NAME_PLACEHOLDER = "#partitionKey";
    public static final String SORT_KEY_NAME_PLACEHOLDER = "#sortKey";
    public static final String PARTITION_KEY_VALUE_PLACEHOLDER = ":partitionKey";
    public static final String SORT_KEY_VALUE_PLACEHOLDER = ":sortKey";

    // #partitionKey = :partitionKey AND #sortKey = :sortKey
    public static final String PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION =
        PARTITION_KEY_NAME_PLACEHOLDER + " = " + PARTITION_KEY_VALUE_PLACEHOLDER
        + " AND "
        + SORT_KEY_NAME_PLACEHOLDER + " = " + SORT_KEY_VALUE_PLACEHOLDER;

    public static final Map<String, String> PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES =
        primaryKeyEqualityConditionAttributeNames();

    public static final String KEY_NOT_EXISTS_CONDITION = keyNotExistsCondition();
    public static final String UNSUPPORTED_KEY_TYPE_EXCEPTION = "Currently only String values are supported";

    private ResourceServiceUtils() {
    }

    static <T> Map<String, AttributeValue> toDynamoFormat(T element) {
        Item item = attempt(() -> Item.fromJSON(objectMapper.writeValueAsString(element))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }

    static <T> T parseAttributeValuesMap(Map<String, AttributeValue> valuesMap, Class<T> dataClass) {
        if (nonNull(valuesMap) && !valuesMap.isEmpty()) {
            Item item = ItemUtils.toItem(valuesMap);
            return attempt(() -> objectMapper.readValue(item.toJSON(), dataClass)).orElseThrow();
        } else {
            throw new EmptyValueMapException();
        }
    }

    static Map<String, AttributeValue> primaryKeyEqualityConditionAttributeValues(ResourceDao resourceDao) {
        return Map.of(PARTITION_KEY_VALUE_PLACEHOLDER,
            new AttributeValue(resourceDao.getPrimaryKeyPartitionKey()),
            SORT_KEY_VALUE_PLACEHOLDER, new AttributeValue(resourceDao.getPrimaryKeySortKey()));
    }

    static Organization newOrganization(URI organizationUri) {
        return new Organization.Builder().withId(organizationUri).build();
    }

    static Organization userOrganization(UserInstance user) {
        return newOrganization(user.getOrganizationUri());
    }

    static TransactWriteItemsRequest newTransactWriteItemsRequest(TransactWriteItem... transaction) {
        return newTransactWriteItemsRequest(Arrays.asList(transaction));
    }

    static TransactWriteItemsRequest newTransactWriteItemsRequest(List<TransactWriteItem> transactionItems) {
        return new TransactWriteItemsRequest().withTransactItems(transactionItems);
    }

    static <T> Map<String, AttributeValue> conditionValueMapToAttributeValueMap(Map<String, Object> valuesMap,
                                                                                Class<T> valueClass) {
        if (String.class.equals(valueClass)) {
            return valuesMap
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        mapEntry -> new AttributeValue((String) mapEntry.getValue())
                    )
                );
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_KEY_TYPE_EXCEPTION);
        }
    }

    static <T extends WithPrimaryKey> TransactWriteItem createTransactionPutEntry(T data, String tableName) {

        Put put = new Put().withItem(toDynamoFormat(data)).withTableName(tableName)
            .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
            .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
        return new TransactWriteItem().withPut(put);
    }

    private static Map<String, String> primaryKeyEqualityConditionAttributeNames() {
        return Map.of(
            PARTITION_KEY_NAME_PLACEHOLDER, PRIMARY_KEY_PARTITION_KEY_NAME,
            SORT_KEY_NAME_PLACEHOLDER, PRIMARY_KEY_SORT_KEY_NAME
        );
    }

    private static String keyNotExistsCondition() {
        return String.format("attribute_not_exists(%s) AND attribute_not_exists(%s)",
            PARTITION_KEY_NAME_PLACEHOLDER, SORT_KEY_NAME_PLACEHOLDER);
    }
}
