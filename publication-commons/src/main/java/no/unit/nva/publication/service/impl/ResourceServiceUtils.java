package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;

public final class ResourceServiceUtils {

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

    static Map<String, AttributeValue> primaryKeyEqualityConditionAttributeValues(WithPrimaryKey resourceDao) {
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
