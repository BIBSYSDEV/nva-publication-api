package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.utils.JsonUtils.objectMapper;
import static nva.commons.utils.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import no.unit.nva.model.Organization;

public final class ResourceServiceUtils {

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Could not find resource";
    public static final String PARTITION_KEY_NAME_PLACEHOLDER = "#partitionKey";
    public static final String SORT_KEY_NAME_PLACEHOLDER = "#sortKey";
    public static final Map<String, String> PRIMARY_KEY_PLACEHOLDERS_AND_ATTRIBUTE_NAMES_MAPPING =
        primaryKeyAttributeNamesMapping();

    public static final String KEY_EXISTS_CONDITION = keyExistsCondition();
    public static final String PARSING_NULL_MAP_ERROR = "Trying to parse null valuesMap";

    private ResourceServiceUtils() {
    }

    static <T> Map<String, AttributeValue> toDynamoFormat(T element) {
        Item item = attempt(() -> Item.fromJSON(objectMapper.writeValueAsString(element))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }


    public static <T> T parseAttributeValuesMap(Map<String,AttributeValue> valuesMap, Class<T> dataClass){
        if(nonNull(valuesMap)){
            Item item = ItemUtils.toItem(valuesMap);
            return attempt(()->objectMapper.readValue(item.toJSON(), dataClass)).orElseThrow();
        }
        else{
            throw new RuntimeException(PARSING_NULL_MAP_ERROR);
        }

    }

    static Organization newOrganization(URI organizationUri) {
        return new Organization.Builder().withId(organizationUri).build();
    }

    static Organization userOrganization(UserInstance user) {
        return newOrganization(user.getOrganizationUri());
    }

    static TransactWriteItem newPutTransactionItem(Put newDataEntry) {
        return new TransactWriteItem().withPut(newDataEntry);
    }

    static TransactWriteItemsRequest newTransactWriteItemsRequest(TransactWriteItem... transaction) {
        return newTransactWriteItemsRequest(Arrays.asList(transaction));
    }

    private static TransactWriteItemsRequest newTransactWriteItemsRequest(List<TransactWriteItem> transactionItems) {
        return new TransactWriteItemsRequest().withTransactItems(transactionItems);
    }

    private static Map<String, String> primaryKeyAttributeNamesMapping() {
        return Map.of(
            PARTITION_KEY_NAME_PLACEHOLDER, PRIMARY_KEY_PARTITION_KEY_NAME,
            SORT_KEY_NAME_PLACEHOLDER, PRIMARY_KEY_SORT_KEY_NAME
        );
    }

    private static String keyExistsCondition() {
        return String.format("attribute_not_exists(%s) AND attribute_not_exists(%s)",
            PARTITION_KEY_NAME_PLACEHOLDER, SORT_KEY_NAME_PLACEHOLDER);
    }
}
