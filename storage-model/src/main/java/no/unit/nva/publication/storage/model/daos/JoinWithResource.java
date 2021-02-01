package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_INDEX_FIELD_PREFIX;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;

@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Resource", value = ResourceDao.class),
    @JsonSubTypes.Type(name = "DoiRequest", value = DoiRequestDao.class),
})
public interface JoinWithResource {

    String LAST_PRINTABLE_ASCII_CHAR = "~";

    @JsonProperty(BY_RESOURCE_INDEX_PARTITION_KEY_NAME)
    default String getByResourcePartitionKey() {
        return
            CUSTOMER_INDEX_FIELD_PREFIX
            + Dao.orgUriToOrgIdentifier(getCustomerId())
            + KEY_FIELDS_DELIMITER
            + RESOURCE_INDEX_FIELD_PREFIX
            + getResourceIdentifier().toString()
            + KEY_FIELDS_DELIMITER;
    }

    @JsonProperty(BY_RESOURCE_INDEX_SORT_KEY_NAME)
    default String getByResourceSortKey() {
        return
            this.getType()
            + KEY_FIELDS_DELIMITER
            + getIdentifier().toString();
    }

    /**
     * Retrieve all entries that are connected to a Resource with types that are alphabetically greater or equal to the
     * left type and less or equal to right type.
     *
     * <p>For example the command:
     *
     * <p>{@code
     *            byResource("DoiRequest", "Resource")
     *    }
     *
     * <p>returns all entries that  are between the types "DoiRequest" and
     * "Resource" including "DoiRequest" and "Resource" and they are connected to the Resource with identifier {@link
     * JoinWithResource#getResourceIdentifier}
     *
     * @param greaterOrEqual the left type.
     * @param lessOrEqual    the right type.
     * @return a Map for using in the
     * {@link com.amazonaws.services.dynamodbv2.model.QueryRequest#withKeyConditions(Map)}
     *     method.
     */
    default Map<String, Condition> byResource(String greaterOrEqual,
                                              String lessOrEqual) {
        Condition partitionKeyCondition = new Condition()
            .withAttributeValueList(new AttributeValue(getByResourcePartitionKey()))
            .withComparisonOperator(ComparisonOperator.EQ);

        Condition sortKeyCondition = new Condition()
            .withAttributeValueList(new AttributeValue(greaterOrEqual),
                new AttributeValue(lessOrEqual + LAST_PRINTABLE_ASCII_CHAR))
            .withComparisonOperator(ComparisonOperator.BETWEEN);

        return Map.of(
            BY_RESOURCE_INDEX_PARTITION_KEY_NAME, partitionKeyCondition,
            BY_RESOURCE_INDEX_SORT_KEY_NAME, sortKeyCondition
        );
    }

    /**
     * Retrieve all entries that are connected to a Resource with types that to the input type.
     *
     * <p>For example the command:
     *
     * <p>{@code
     *            byResource("Message")
     *    }
     *
     * <p>returns all entries that  are between the type "Message" and they are connected to the Resource
     * with identifier {@link JoinWithResource#getResourceIdentifier}
     *
     * @param selectedType the input type.
     * @return a Map for using in the
     * {@link com.amazonaws.services.dynamodbv2.model.QueryRequest#withKeyConditions(Map)}
     *     method.
     */
    default Map<String, Condition> byResource(String selectedType) {
        Condition partitionKeyCondition = new Condition()
            .withAttributeValueList(new AttributeValue(getByResourcePartitionKey()))
            .withComparisonOperator(ComparisonOperator.EQ);

        Condition sortKeyCondition = new Condition()
            .withAttributeValueList(new AttributeValue(selectedType))
            .withComparisonOperator(ComparisonOperator.BEGINS_WITH);

        return Map.of(
            BY_RESOURCE_INDEX_PARTITION_KEY_NAME, partitionKeyCondition,
            BY_RESOURCE_INDEX_SORT_KEY_NAME, sortKeyCondition
        );
    }

    SortableIdentifier getIdentifier();

    String getType();

    @JsonIgnore
    SortableIdentifier getResourceIdentifier();

    URI getCustomerId();
}
