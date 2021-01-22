package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.STATUS_INDEX_FIELD_PREFIX;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.MalformedURLException;
import java.util.stream.Stream;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceTest;
import no.unit.nva.publication.storage.model.WithStatus;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DaoTest {

    public static ResourceTest resourceGenerator = new ResourceTest();

    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void getTypeReturnsNameOfTheContainedObject(Dao<?> daoInstance) {
        String expectedType = daoInstance.getData().getClass().getSimpleName();
        assertThat(daoInstance.getType(), is(equalTo(expectedType)));
    }

    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void getIdentifierReturnsTheIdentifierOfTheContainedObject(Dao<?> daoInstance) {
        String expectedIdentifier = daoInstance.getData().getIdentifier().toString();
        assertThat(expectedIdentifier, is(not(emptyString())));

        assertThat(daoInstance.getIdentifier(), is(equalTo(expectedIdentifier)));
    }

    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void getCustomerIdReturnsTheCustomerIdOfTheContainedObject(Dao<?> dao) {
        String expectedCustomerId = dao.getData().getCustomerId().toString();
        assertThat(expectedCustomerId, is(not(emptyString())));

        assertThat(dao.getCustomerId(), is(equalTo(expectedCustomerId)));
    }

    @ParameterizedTest
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

    @ParameterizedTest
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
    @MethodSource("instanceProvider")
    public void daoByCustomerAndStatusIndexPartitionKeyContainsOnlyTypeCustomerIdentifierAndStatusInThatOrder
        (Dao<? extends WithStatus> dao) throws JsonProcessingException {
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

    @ParameterizedTest
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

    private static Stream<Dao<?>> instanceProvider() throws InvalidIssnException, MalformedURLException {
        return Stream.of(resourceDao());
    }

    private static ResourceDao resourceDao() throws InvalidIssnException, MalformedURLException {
        return Try.of(resourceGenerator.samplePublication(
            resourceGenerator.sampleJournalArticleReference()))
            .map(Resource::fromPublication)
            .map(ResourceDao::new)
            .orElseThrow();
    }

    private JsonNode serializeInstance(Dao<?> daoInstance) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(daoInstance);
        return objectMapper.readTree(json);
    }
}