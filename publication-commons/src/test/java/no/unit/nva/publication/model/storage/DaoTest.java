package no.unit.nva.publication.model.storage;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
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
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.startsWith;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DaoTest extends ResourcesLocalTest {
    
    public static Stream<Class<?>> entityProvider() {
        return TypeProvider.listSubTypes(Entity.class);
    }
    
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
            daoInstance.getOwner().toString()
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
    
    @ParameterizedTest(name = "Dao type:{0}")
    @DisplayName("should generate a new version whenever it is instantiated through a Business Object")
    @MethodSource("entityProvider")
    void shouldGenerateNewVersionWheneverIsInstantiatedThroughBusinessObject(Class<?> entityType)
        throws ConflictException {
        var entity = (Entity) generateEntity(entityType);
        var dao = entity.toDao();
        var dao2 = entity.toDao();
        assertThat(dao.getVersion(), is(not(nullValue())));
        assertThat(dao2.getVersion(), is(not(nullValue())));
        assertThat(dao.getVersion(), is(not(equalTo(dao2.getVersion()))));
    }
    
    @ParameterizedTest(name = "Dao type:{0}")
    @DisplayName("dao has only type, data, version, PK, and SK fields")
    @MethodSource("entityProvider")
    void daoOnlyHasOnlyTypeDataVersionPKAndSKFields(Class<?> entityType)
        throws JsonProcessingException, ConflictException {
        var entity = (Entity) generateEntity(entityType);
        var dao = entity.toDao();
        String stringValue = dynamoDbObjectMapper.writeValueAsString(dao);
        ObjectNode jsonNode = (ObjectNode) dynamoDbObjectMapper.readTree(stringValue);
        Iterator<String> fieldNames = jsonNode.fieldNames();
        List<String> fieldNameList = new ArrayList<>();
        fieldNames.forEachRemaining(fieldNameList::add);
        assertThat(fieldNameList, everyItem(anyOf(
            startsWith("PK"),
            startsWith("SK"),
            startsWith("data"),
            startsWith("type"),
            startsWith("version")))
        );
    }
    
    @SuppressWarnings("unchecked")
    private Object generateEntity(Class<?> entityType)
        throws ConflictException {
        
        if (Resource.class.equals(entityType)) {
            return Resource.fromPublication(randomPublication());
        } else if (DoiRequest.class.equals(entityType)) {
            return createTicket((Class<? extends TicketEntry>) entityType);
        } else if (PublishingRequestCase.class.equals(entityType)) {
            return createTicket((Class<? extends TicketEntry>) entityType);
        } else if (GeneralSupportRequest.class.equals(entityType)) {
            return createTicket((Class<? extends TicketEntry>) entityType);
        } else if (Message.class.equals(entityType)) {
            var ticket = createTicket(DoiRequest.class);
            return Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        }
        throw new UnsupportedOperationException();
    }
    
    private static TicketEntry createTicket(Class<? extends TicketEntry> entityType) throws ConflictException {
        return TicketEntry.createNewTicket(draftPublicationWithoutDoi(), entityType, SortableIdentifier::next);
    }
    
    private static Publication draftPublicationWithoutDoi() {
        return randomPublication()
                   .copy()
                   .withStatus(PublicationStatus.DRAFT)
                   .withDoi(null)
                   .build();
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