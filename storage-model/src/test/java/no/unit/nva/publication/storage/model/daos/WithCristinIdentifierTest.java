package no.unit.nva.publication.storage.model.daos;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.sampleResourceDao;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.toPutItemRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class WithCristinIdentifierTest extends ResourcesDynamoDbLocalTest {

    public void init() {
        super.init();
    }

    @ParameterizedTest
    @MethodSource("withPrimaryKeyInstancesProvider")
    public void dynamoDbClientReturnsItemWithMatchingCristinId(List<ResourceDao> daos) {
        init();
        List<WithCristinIdentifier> results = new ArrayList<>();
        daos.forEach(dao -> client.putItem(toPutItemRequest(dao)));
        daos.forEach(dao -> results.add(queryDB(dao.getResourceByCristinIdPartitionKey(), dao)));
        results.forEach(System.out::println);
        daos.forEach(System.out::println);
        var expectedItems = daos.toArray(Object[]::new);
        assertThat(results, containsInAnyOrder(expectedItems));
    }

    private static Stream<List<ResourceDao>> withPrimaryKeyInstancesProvider()
            throws InvalidIssnException, MalformedURLException {
        List<ResourceDao> resources = sampleResourceDaoList(3);
        return Stream.of(resources);
    }

    private static List<ResourceDao> sampleResourceDaoList(int sizeOfList) throws MalformedURLException, InvalidIssnException {
        ArrayList<ResourceDao> resources = new ArrayList<ResourceDao>();
        for (int i = 0; i < sizeOfList; i++) {
            resources.add(sampleResourceDao());
        }
        return resources;
    }

    private QueryRequest createQuery(String cristinId) {
        return new QueryRequest()
                .withTableName(RESOURCES_TABLE_NAME)
                .withIndexName(RESOURCE_BY_CRISTIN_ID_INDEX_NAME)
                .withKeyConditions(createConditionsWithCristinId(cristinId));
    }

    private WithCristinIdentifier queryDB(String cristinId, WithCristinIdentifier dao) {
        QueryRequest queryRequest = createQuery(cristinId);
        return client.query(queryRequest)
                .getItems()
                .stream()
                .map(item -> DynamoEntry.parseAttributeValuesMap(item, dao.getClass()))
                .collect(SingletonCollector.collectOrElse(null));
    }

    private Map<String, Condition> createConditionsWithCristinId(String cristinId) {
        Condition condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue(cristinId));
        return Map.of(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME, condition);
    }
}
