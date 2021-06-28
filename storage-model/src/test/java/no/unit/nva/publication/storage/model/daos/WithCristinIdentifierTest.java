package no.unit.nva.publication.storage.model.daos;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.Map;

import static no.unit.nva.publication.storage.model.DatabaseConstants.CRISTIN_ID_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.NULL_VALUE_KEY;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.sampleResourceDao;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.toPutItemRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class WithCristinIdentifierTest extends ResourcesDynamoDbLocalTest {

    @BeforeEach
    public void init() {
        super.init();
    }

    @Test
    public void dynamoClientReturnsResourceWithMatchingCristinIdWhenSearchingResourcesByCristinId() throws MalformedURLException, InvalidIssnException {
        ResourceDao dao = sampleResourceDao();
        client.putItem(toPutItemRequest(dao));
        WithCristinIdentifier actuallResult = queryDbFindByCristinIdentifier(dao);
        WithCristinIdentifier expectedItem = dao;
        assertThat(actuallResult, is(equalTo(expectedItem)));
    }

    @Test
    public void dynamoClientReturnsOnlyResourcesWithCristinIdWhenSearchingResourcesByCristinId() throws MalformedURLException, InvalidIssnException {
        ResourceDao daoWithCristinId = sampleResourceDao();
        ResourceDao daoWithoutCristinId = new ResourceDao(sampleResourceDao().getData().copy().withAdditionalIdentifiers(null).build());
        client.putItem(toPutItemRequest(daoWithCristinId));
        client.putItem(toPutItemRequest(daoWithoutCristinId));
        ScanResult result = client.scan(
                new ScanRequest()
                        .withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                        .withIndexName(RESOURCE_BY_CRISTIN_ID_INDEX_NAME)
                        .withScanFilter(createConditionsWithCristinIdentifier()));
        int expectedResultCounter = 1;
        assertThat(result.getCount(), is(equalTo(expectedResultCounter)));
    }

    private Map<String, Condition> createConditionsWithCristinIdentifier() {
        Condition condition = new Condition()
                .withComparisonOperator(ComparisonOperator.NOT_CONTAINS)
                .withAttributeValueList(new AttributeValue(CRISTIN_ID_INDEX_FIELD_PREFIX + KEY_FIELDS_DELIMITER + NULL_VALUE_KEY));
        return Map.of(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME, condition);
    }

    private WithCristinIdentifier queryDbFindByCristinIdentifier(WithCristinIdentifier dao) {
        QueryRequest queryRequest = dao.createQueryFindByCristinIdentifier();
        return client.query(queryRequest)
                .getItems()
                .stream()
                .map(item -> DynamoEntry.parseAttributeValuesMap(item, dao.getClass()))
                .collect(SingletonCollector.collectOrElse(null));
    }

}
