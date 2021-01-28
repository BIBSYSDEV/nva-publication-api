package no.unit.nva.publication.storage.model.daos;

import static java.util.Objects.nonNull;
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
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.MalformedURLException;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceTest;
import no.unit.nva.publication.storage.model.RowLevelSecurity;
import no.unit.nva.publication.storage.model.WithIdentifier;
import no.unit.nva.publication.storage.model.WithStatus;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DaoTest extends ResourcesDynamoDbLocalTest {

    public static final String EMPTY_VALUE_ERROR = "ValueMap was either null or empty";
    public static ResourceTest resourceGenerator = new ResourceTest();
    Javers javers = JaversBuilder.javers().build();

    @BeforeEach
    public void init() {
        super.init();
    }

    @ParameterizedTest(name = "get Type Returns Name Of The Contained Object: {0}")
    @MethodSource("instanceProvider")
    public void getTypeReturnsNameOfTheContainedObject(Dao<?> daoInstance) {
        String expectedType = daoInstance.getData().getClass().getSimpleName();
        assertThat(daoInstance.getType(), is(equalTo(expectedType)));
    }

    @ParameterizedTest(name = "get Identifier Returns TheIdentifier Of The Contained Object {0}")
    @MethodSource("instanceProvider")
    public void getIdentifierReturnsTheIdentifierOfTheContainedObject(Dao<?> daoInstance) {
        String expectedIdentifier = daoInstance.getData().getIdentifier().toString();
        assertThat(expectedIdentifier, is(not(emptyString())));

        assertThat(daoInstance.getIdentifier(), is(equalTo(expectedIdentifier)));
    }

    @ParameterizedTest(name = "getCustomerId Returns TheCustomerI dOfThe Contained Object:{0}")
    @MethodSource("instanceProvider")
    public void getCustomerIdReturnsTheCustomerIdOfTheContainedObject(Dao<?> dao) {
        String expectedCustomerId = dao.getData().getCustomerId().toString();
        assertThat(expectedCustomerId, is(not(emptyString())));

        assertThat(dao.getCustomerId().toString(), is(equalTo(expectedCustomerId)));
    }

    @ParameterizedTest(name = "daoPrimaryKeyPartitionKeyContainsOnlyTypeCustomerIdentifierAndOwnerInThatOrder:{0}")
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

    @ParameterizedTest(name = "daoPrimaryKeySortKeyContainsOnlyTypeAndIdentifierInThatOrder:{0}")
    @MethodSource("instanceProvider")
    public void daoPrimaryKeySortKeyContainsOnlyTypeAndIdentifierInThatOrder(Dao<?> daoInstance)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(daoInstance);
        assertThat(jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME), is(not(nullValue())));
        String primaryKeySortKey = jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME).textValue();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            daoInstance.getType(),
            daoInstance.getIdentifier());
        assertThat(primaryKeySortKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest
        (name = "daoByCustomerAndStatusIndexPartitionKeyContainsOnlyTypeCustomerIdentifierAndStatusInThatOrder:{0}")
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
            dao.getData().getStatus());

        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest(name = "daoByCustomerAndStatusIndexSortKeyContainsOnlyTypeAndIdentifier:{0}")
    @MethodSource("instanceProvider")
    public void daoByCustomerAndStatusIndexSortKeyContainsOnlyTypeAndIdentifier(Dao<? extends WithStatus> dao)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(dao);
        assertThat(jsonNode.get(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME), is(not(nullValue())));
        String byTypeCustomerStatusIndexPartitionKey = dao.getByTypeCustomerStatusSortKey();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            dao.getType(),
            dao.getIdentifier());

        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest(name = "daoCanBeRetrievedByPrimaryKeyFromDynamo:{0}")
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

    @ParameterizedTest
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
        ResourceDao resourceDao = resourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao();
        return Stream.of(resourceDao, doiRequestDao);
    }

    private static DoiRequestDao doiRequestDao() {
        return attempt(DaoTest::sampleResource)
            .map(resource -> DoiRequest.fromResource(resource, Clock.systemDefaultZone()))
            .map(DoiRequestDao::new)
            .orElseThrow();
    }

    private static Resource sampleResource() throws InvalidIssnException, MalformedURLException {
        return Resource.fromPublication(resourceGenerator.samplePublication(
            resourceGenerator.sampleJournalArticleReference()));
    }

    private static ResourceDao resourceDao() throws InvalidIssnException, MalformedURLException {
        return Try.of(sampleResource())
            .map(ResourceDao::new)
            .orElseThrow();
    }

    private static <T> Map<String, AttributeValue> toDynamoFormat(T element) {
        Item item = attempt(() -> Item.fromJSON(objectMapper.writeValueAsString(element))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }

    private static <T> T parseAttributeValuesMap(Map<String, AttributeValue> valuesMap, Class<T> dataClass) {
        if (nonNull(valuesMap) && !valuesMap.isEmpty()) {
            Item item = ItemUtils.toItem(valuesMap);
            return attempt(() -> objectMapper.readValue(item.toJSON(), dataClass)).orElseThrow();
        } else {
            throw new RuntimeException(EMPTY_VALUE_ERROR);
        }
    }

    private QueryRequest queryByTypeCustomerStatusIndex(Dao<?> originalResource) {
        return new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
            .withKeyConditions(originalResource.byTypeCustomerStatusKey());
    }

    private <R extends WithIdentifier & RowLevelSecurity> PutItemRequest toPutItemRequest(Dao<R> resource) {
        return new PutItemRequest().withTableName(RESOURCES_TABLE_NAME)
            .withItem(toDynamoFormat(resource));
    }

    private JsonNode serializeInstance(Dao<?> daoInstance) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(daoInstance);
        return objectMapper.readTree(json);
    }
}