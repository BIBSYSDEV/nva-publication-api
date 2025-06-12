package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
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
    @JsonSubTypes.Type(name = "PublishingRequest", value = PublishingRequestDao.class),
    @JsonSubTypes.Type(name = "UnpublishRequest", value = UnpublishRequestDao.class),
    @JsonSubTypes.Type(name = "MessageDao", value = MessageDao.class),
    @JsonSubTypes.Type(name = FileDao.TYPE, value = FileDao.class),
})
public interface JoinWithResource {
    
    String LAST_PRINTABLE_ASCII_CHAR = "~";
    
    @JsonProperty(BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME)
    default String getByCustomerAndResourcePartitionKey() {
        return
            CUSTOMER_INDEX_FIELD_PREFIX
            + KEY_FIELDS_DELIMITER
            + Dao.orgUriToOrgIdentifier(getCustomerId())
            + KEY_FIELDS_DELIMITER
            + RESOURCE_INDEX_FIELD_PREFIX
            + KEY_FIELDS_DELIMITER
            + getResourceIdentifier().toString();
    }
    
    @JsonProperty(BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME)
    default String getByCustomerAndResourceSortKey() {
        return
            this.joinByResourceOrderedType()
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
     * byResource("DoiRequest", "Resource") }
     *
     * <p>returns all entries that  are between the types "DoiRequest" and
     * "Resource" including "DoiRequest" and "Resource" and they are connected to the Resource with identifier
     * {@link JoinWithResource#getResourceIdentifier}
     *
     * @param greaterOrEqual the left type.
     * @param lessOrEqual    the right type.
     * @return a Map for using in the
     *     {@link com.amazonaws.services.dynamodbv2.model.QueryRequest#withKeyConditions(Map)} method.
     */
    default Map<String, Condition> byResource(String greaterOrEqual,
                                              String lessOrEqual) {
    
        Condition partitionKeyCondition = new Condition()
                                              .withAttributeValueList(
                                                  new AttributeValue(getByCustomerAndResourcePartitionKey()))
                                              .withComparisonOperator(ComparisonOperator.EQ);
    
        Condition sortKeyCondition = new Condition()
                                         .withAttributeValueList(new AttributeValue(greaterOrEqual),
                                             new AttributeValue(lessOrEqual + LAST_PRINTABLE_ASCII_CHAR))
                                         .withComparisonOperator(ComparisonOperator.BETWEEN);
        return Map.of(
            BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME, partitionKeyCondition,
            BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME, sortKeyCondition
        );
    }
    
    /**
     * Retrieve all entries that are connected to a Resource with type equal to the input type.
     *
     * <p>For example the command:
     *
     * <p>{@code
     * byResource("Message") }
     *
     * <p>returns all entries that  are between the type "Message" and they are connected to the Resource
     * with identifier {@link JoinWithResource#getResourceIdentifier}
     *
     * @param selectedType the input type.
     * @return a Map for using in the
     *     {@link com.amazonaws.services.dynamodbv2.model.QueryRequest#withKeyConditions(Map)} method. #HashKey =
     *     :ByResourceIndexHashKey (Customer:SomeCustomerId:Resource:SomeResourceId) AND #SortKey begins_with
     *     :ByResourceIndexSortKey (d:Message:SomeId)
     */
    //TODO: type should be an enum
    default Map<String, Condition> byResource(String selectedType) {
        
        Condition partitionKeyCondition = new Condition()
                                              .withAttributeValueList(
                                                  new AttributeValue(getByCustomerAndResourcePartitionKey()))
                                              .withComparisonOperator(ComparisonOperator.EQ);
        
        Condition sortKeyCondition = new Condition()
                                         .withAttributeValueList(new AttributeValue(selectedType))
                                         .withComparisonOperator(ComparisonOperator.BEGINS_WITH);
        
        return Map.of(
            BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME, partitionKeyCondition,
            BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME, sortKeyCondition
        );
    }
    
    default Map<String, Condition> joinAllRelatedTicketsForResource() {
        return byResource(TicketDao.ALPHABETICALLY_ORDERED_FIRST_TICKET_TYPE,
            TicketDao.ALPHABETICALLY_ORDERED_LAST_TICKET_TYPE);
    }

    SortableIdentifier getIdentifier();
    
    /**
     * The type of the Entry (e.g. Resource, DoiRequest, etc) prefixed with a letter in order to impose an order when we
     * present all Entries connected to a Resource.
     *
     * <p>Example: <br/>If the orderedType for a Resource is a:Resource, for a DoiRequest is b:DoiRequest and for
     * a Message is z:Message, then, when we list all entries associated with a Resource, we would get the entries
     * ordered as shown below:*
     * <ol>
     *    <li>Resource</li>
     *    <li>DoiRequest</li>
     *    <li>Message</li>
     * </ol>
     *
     * @return the type of the entry prefixed with a letter imposing an order when entries are joined by Resource
     */
    @JsonIgnore
    String joinByResourceOrderedType();
    
    @JsonIgnore
    SortableIdentifier getResourceIdentifier();
    
    URI getCustomerId();
    
    final class Constants {
        
        public static final int DOI_REQUEST_INDEX_IN_QUERY_RESULT = 1;
        public static final int RESOURCE_INDEX_IN_QUERY_RESULT = 0;
        
        private Constants() {
        
        }
    }
}
