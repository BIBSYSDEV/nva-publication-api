package no.unit.nva.publication.model.storage;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.DaoUtils.toPutItemRequest;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.STATUS_INDEX_FIELD_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DaoTest extends ResourcesLocalTest {
    
    @BeforeEach
    public void init() {
        super.init();
    }
    
    @ParameterizedTest(name = "dataType returns name of the contained object: {0}")
    @MethodSource("instanceProvider")
    void getTypeReturnsNameOfTheContainedObject(Dao daoInstance) {
        String expectedType = daoInstance.getData().getClass().getSimpleName();
        assertThat(daoInstance.dataType(), is(equalTo(expectedType)));
    }
    
    @ParameterizedTest(name = "getIdentifier returns the identifier of the contained object: {0}")
    @MethodSource("instanceProvider")
    void getIdentifierReturnsTheIdentifierOfTheContainedObject(Dao daoInstance) {
        String expectedIdentifier = daoInstance.getData().getIdentifier().toString();
        assertThat(expectedIdentifier, is(not(emptyString())));
        
        assertThat(daoInstance.getIdentifier().toString(), is(equalTo(expectedIdentifier)));
    }
    
    @ParameterizedTest(name = "getCustomerId returns the customerId of the contained object: {0}")
    @MethodSource("instanceProvider")
    void getCustomerIdReturnsTheCustomerIdOfTheContainedObject(Dao dao) {
        String expectedCustomerId = dao.getData().getCustomerId().toString();
        assertThat(expectedCustomerId, is(not(emptyString())));
        
        assertThat(dao.getCustomerId().toString(), is(equalTo(expectedCustomerId)));
    }
    
    @ParameterizedTest(name = "daoPrimaryKeyPartitionKey contains only Type, CustomerIdentifier, and Owner "
                              + "in that order: {0}")
    @MethodSource("instanceProvider")
    void daoPrimaryKeyPartitionKeyContainsOnlyTypeCustomerIdentifierAndOwnerInThatOrder(Dao daoInstance)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(daoInstance);
        
        assertThat(jsonNode.get(PRIMARY_KEY_PARTITION_KEY_NAME), is(not(nullValue())));
        String primaryKeyPartitionKey = jsonNode.get(PRIMARY_KEY_PARTITION_KEY_NAME).textValue();
        
        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            daoInstance.indexingType(),
            daoInstance.getCustomerIdentifier(),
            daoInstance.getOwner()
        );
        
        assertThat(primaryKeyPartitionKey, is(equalTo(expectedFormat)));
    }
    
    @ParameterizedTest(name = "daoPrimaryKeySortKey contains only Type and Identifier in that order: {0}")
    @MethodSource("instanceProvider")
    void daoPrimaryKeySortKeyContainsOnlyTypeAndIdentifierInThatOrder(Dao daoInstance)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(daoInstance);
        assertThat(jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME), is(not(nullValue())));
        String primaryKeySortKey = jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME).textValue();
        
        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            daoInstance.indexingType(),
            daoInstance.getIdentifier().toString());
        assertThat(primaryKeySortKey, is(equalTo(expectedFormat)));
    }
    
    @ParameterizedTest
        (name = "daoByCustomerAndStatusIndexPartitionKey contains only Type, CustomerIdentifier and Status "
                + "in that order: {0}")
    @MethodSource("instanceProvider")
    void daoByCustomerAndStatusIndexPartitionKeyContainsOnlyTypeCustomerIdentifierAndStatusInThatOrder(Dao dao)
        throws JsonProcessingException {
        
        JsonNode jsonNode = serializeInstance(dao);
        assertThat(jsonNode.get(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME), is(not(nullValue())));
        String byTypeCustomerStatusIndexPartitionKey = dao.getByTypeCustomerStatusPartitionKey();
        
        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            dao.indexingType(),
            CUSTOMER_INDEX_FIELD_PREFIX,
            dao.getCustomerIdentifier(),
            STATUS_INDEX_FIELD_PREFIX,
            dao.getData().getStatusString());
        
        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }
    
    @ParameterizedTest(name = "daoByCustomerAndStatusIndexSortKey contains only type and identifier: {0}")
    @MethodSource("instanceProvider")
    void daoByCustomerAndStatusIndexSortKeyContainsOnlyTypeAndIdentifier(Dao dao)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(dao);
        assertThat(jsonNode.get(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME), is(not(nullValue())));
        String byTypeCustomerStatusIndexPartitionKey = dao.getByTypeCustomerStatusSortKey();
        
        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            dao.indexingType(),
            dao.getIdentifier().toString());
        
        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }
    
    @ParameterizedTest(name = "dao can be retrieved by primary-key from dynamo: {0}")
    @MethodSource("instanceProvider")
    void daoCanBeRetrievedByPrimaryKeyFromDynamo(Dao originalResource) {
        
        client.putItem(toPutItemRequest(originalResource));
        GetItemResult getItemResult = client.getItem(
            new GetItemRequest().withTableName(RESOURCES_TABLE_NAME)
                .withKey(originalResource.primaryKey()));
        Dao retrievedResource = parseAttributeValuesMap(getItemResult.getItem(), originalResource.getClass());
        
        assertThat(originalResource, doesNotHaveEmptyValues());
        assertThat(originalResource, is(equalTo(retrievedResource)));
    }
    
    @ParameterizedTest(name = "dao can be retrieved by the ByTypePublisherStatus index: {0}")
    @MethodSource("instanceProvider")
    void daoCanBeRetrievedByTypePublisherStatusIndex(Dao originalDao) {
        client.putItem(toPutItemRequest(originalDao));
        QueryResult queryResult = client.query(queryByTypeCustomerStatusIndex(originalDao));
        Dao retrievedDao = queryResult.getItems()
                               .stream()
                               .map(map -> parseAttributeValuesMap(map, originalDao.getClass()))
                               .collect(SingletonCollector.collect());
        assertThat(retrievedDao, is(equalTo(originalDao)));
    }
    
    @ParameterizedTest
    @MethodSource("instanceProvider")
    void parseAttributeValuesMapCreatesDaoWithoutLossOfInformation(Dao originalDao) {
        
        assertThat(originalDao, doesNotHaveEmptyValues());
        Map<String, AttributeValue> dynamoMap = originalDao.toDynamoFormat();
        Dao parsedDao = parseAttributeValuesMap(dynamoMap, originalDao.getClass());
        assertThat(parsedDao, is(equalTo(originalDao)));
    }
    
    @ParameterizedTest(name = "toDynamoFormat creates a Dynamo object preserving all information")
    @MethodSource("instanceProvider")
    void toDynamoFormatCreatesADynamoJsonFormatObjectPreservingAllInformation(Dao originalDao) {
        
        Map<String, AttributeValue> dynamoMap = originalDao.toDynamoFormat();
        client.putItem(RESOURCES_TABLE_NAME, dynamoMap);
        Map<String, AttributeValue> savedMap = client
            .getItem(RESOURCES_TABLE_NAME, originalDao.primaryKey())
            .getItem();
        assertThat(dynamoMap, is(equalTo(savedMap)));
        
        Dao retrievedDao = parseAttributeValuesMap(savedMap, originalDao.getClass());
        assertThat(retrievedDao, doesNotHaveEmptyValues());
        assertThat(retrievedDao, is(equalTo(originalDao)));
    }
    
    private static Stream<Dao> instanceProvider() {
        return DaoUtils.instanceProvider();
    }
    
    private QueryRequest queryByTypeCustomerStatusIndex(Dao originalResource) {
        return new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
            .withKeyConditions(originalResource.fetchEntryByTypeCustomerStatusKey());
    }
    
    private JsonNode serializeInstance(Dao daoInstance) throws JsonProcessingException {
        String json = dynamoDbObjectMapper.writeValueAsString(daoInstance);
        return dynamoDbObjectMapper.readTree(json);
    }
}