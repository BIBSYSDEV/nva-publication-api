package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.STATUS_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.toPutItemRequest;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.MalformedURLException;
import java.util.stream.Stream;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.WithStatus;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DaoTest extends ResourcesDynamoDbLocalTest {

    @BeforeEach
    public void init() {
        super.init();
    }

    @ParameterizedTest(name = "getType returns name of the contained object: {0}")
    @MethodSource("instanceProvider")
    public void getTypeReturnsNameOfTheContainedObject(Dao<?> daoInstance) {
        String expectedType = daoInstance.getData().getClass().getSimpleName();
        assertThat(daoInstance.getType(), is(equalTo(expectedType)));
    }

    @ParameterizedTest(name = "getIdentifier returns the identifier of the contained object: {0}")
    @MethodSource("instanceProvider")
    public void getIdentifierReturnsTheIdentifierOfTheContainedObject(Dao<?> daoInstance) {
        String expectedIdentifier = daoInstance.getData().getIdentifier().toString();
        assertThat(expectedIdentifier, is(not(emptyString())));

        assertThat(daoInstance.getIdentifier().toString(), is(equalTo(expectedIdentifier)));
    }

    @ParameterizedTest(name = "getCustomerId returns the customerId of the contained object: {0}")
    @MethodSource("instanceProvider")
    public void getCustomerIdReturnsTheCustomerIdOfTheContainedObject(Dao<?> dao) {
        String expectedCustomerId = dao.getData().getCustomerId().toString();
        assertThat(expectedCustomerId, is(not(emptyString())));

        assertThat(dao.getCustomerId().toString(), is(equalTo(expectedCustomerId)));
    }

    @ParameterizedTest(name = "daoPrimaryKeyPartitionKey contains only Type, CustomerIdentifier, and Owner "
                              + "in that order: {0}")
    @MethodSource("instanceProvider")
    public void daoPrimaryKeyPartitionKeyContainsOnlyTypeCustomerIdentifierAndOwnerInThatOrder(Dao<?> daoInstance)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(daoInstance);

        assertThat(jsonNode.get(PRIMARY_KEY_PARTITION_KEY_NAME), is(not(nullValue())));
        String primaryKeyPartitionKey = jsonNode.get(PRIMARY_KEY_PARTITION_KEY_NAME).textValue();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            daoInstance.getType(),
            daoInstance.getCustomerIdentifier(),
            daoInstance.getOwner()
        );

        assertThat(primaryKeyPartitionKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest(name = "daoPrimaryKeySortKey contains only Type and Identifier in that order: {0}")
    @MethodSource("instanceProvider")
    public void daoPrimaryKeySortKeyContainsOnlyTypeAndIdentifierInThatOrder(Dao<?> daoInstance)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(daoInstance);
        assertThat(jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME), is(not(nullValue())));
        String primaryKeySortKey = jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME).textValue();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            daoInstance.getType(),
            daoInstance.getIdentifier().toString());
        assertThat(primaryKeySortKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest
        (name = "daoByCustomerAndStatusIndexPartitionKey contains only Type, CustomerIdentifier and Status "
                + "in that order: {0}")
    @MethodSource("instanceProvider")
    public void daoByCustomerAndStatusIndexPartitionKeyContainsOnlyTypeCustomerIdentifierAndStatusInThatOrder(
        Dao<? extends WithStatus> dao) throws JsonProcessingException {

        JsonNode jsonNode = serializeInstance(dao);
        assertThat(jsonNode.get(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME), is(not(nullValue())));
        String byTypeCustomerStatusIndexPartitionKey = dao.getByTypeCustomerStatusPartitionKey();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            dao.getType(),
            CUSTOMER_INDEX_FIELD_PREFIX,
            dao.getCustomerIdentifier(),
            STATUS_INDEX_FIELD_PREFIX,
            dao.getData().getStatusString());

        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest(name = "daoByCustomerAndStatusIndexSortKey contains only type and identifier: {0}")
    @MethodSource("instanceProvider")
    public void daoByCustomerAndStatusIndexSortKeyContainsOnlyTypeAndIdentifier(Dao<? extends WithStatus> dao)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(dao);
        assertThat(jsonNode.get(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME), is(not(nullValue())));
        String byTypeCustomerStatusIndexPartitionKey = dao.getByTypeCustomerStatusSortKey();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            dao.getType(),
            dao.getIdentifier().toString());

        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest(name = "dao can be retrieved by primary-key from dynamo: {0}")
    @MethodSource("instanceProvider")
    public void daoCanBeRetrievedByPrimaryKeyFromDynamo(Dao<?> originalResource) {

        client.putItem(toPutItemRequest(originalResource));
        GetItemResult getItemResult = client.getItem(
            new GetItemRequest().withTableName(RESOURCES_TABLE_NAME)
                .withKey(originalResource.primaryKey()));
        Dao<?> retrievedResource = parseAttributeValuesMap(getItemResult.getItem(), originalResource.getClass());

        assertThat(originalResource, doesNotHaveEmptyValues());
        assertThat(originalResource, is(equalTo(retrievedResource)));
    }
    
    @ParameterizedTest(name = "dao can be retrieved by the ByTypePublisherStatus index: {0}")
    @MethodSource("instanceProvider")
    public void daoCanBeRetrievedByTypePublisherStatusIndex(Dao<?> originalDao) {
        client.putItem(toPutItemRequest(originalDao));
        QueryResult queryResult = client.query(queryByTypeCustomerStatusIndex(originalDao));
        Dao<?> retrievedDao = queryResult.getItems()
            .stream()
            .map(map -> parseAttributeValuesMap(map, originalDao.getClass()))
            .collect(SingletonCollector.collect());
        assertThat(retrievedDao, is(equalTo(originalDao)));
    }

    private static Stream<Dao<?>> instanceProvider() throws InvalidIssnException, MalformedURLException {
        return DaoUtils.instanceProvider();
    }

    private QueryRequest queryByTypeCustomerStatusIndex(Dao<?> originalResource) {
        return new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
            .withKeyConditions(originalResource.byTypeCustomerStatusKey());
    }

    private JsonNode serializeInstance(Dao<?> daoInstance) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(daoInstance);
        return objectMapper.readTree(json);
    }
}