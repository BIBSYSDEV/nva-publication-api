package no.unit.nva.publication.storage.model.daos;

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
    public void dynamoClientReturnsResourceWithMatchingCristinIdWhenSearchingResourcesByCristinId()
            throws MalformedURLException, InvalidIssnException {
        ResourceDao dao = sampleResourceDao();
        client.putItem(toPutItemRequest(dao));
        WithCristinIdentifier actualResult = queryDbFindByCristinIdentifier(dao);
        WithCristinIdentifier expectedItem = dao;
        assertThat(actualResult, is(equalTo(expectedItem)));
    }

    @Test
    public void dynamoClientReturnsOnlyResourcesWithCristinIdWhenSearchingResourcesByCristinId()
            throws MalformedURLException, InvalidIssnException {
        ResourceDao daoWithCristinId = sampleResourceDao();
        ResourceDao daoWithoutCristinId = createResourceDaoWithoutCristinIdentifier();
        client.putItem(toPutItemRequest(daoWithCristinId));
        client.putItem(toPutItemRequest(daoWithoutCristinId));
        ScanResult result = client.scan(
                new ScanRequest()
                        .withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                        .withIndexName(RESOURCE_BY_CRISTIN_ID_INDEX_NAME));
        assertThat(result.getCount(), is(equalTo(1)));
    }


    private WithCristinIdentifier queryDbFindByCristinIdentifier(WithCristinIdentifier dao) {
        QueryRequest queryRequest = dao.createQueryFindByCristinIdentifier();
        return client.query(queryRequest)
                .getItems()
                .stream()
                .map(item -> DynamoEntry.parseAttributeValuesMap(item, dao.getClass()))
                .collect(SingletonCollector.collectOrElse(null));
    }

    protected static ResourceDao createResourceDaoWithoutCristinIdentifier()
            throws MalformedURLException, InvalidIssnException {
        return new ResourceDao(sampleResourceDao()
                .getData()
                .copy()
                .withAdditionalIdentifiers(null)
                .build());
    }

}
