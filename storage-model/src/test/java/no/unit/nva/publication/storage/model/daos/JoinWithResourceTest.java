package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.doiRequestDao;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.sampleResourceDao;
import static no.unit.nva.publication.storage.model.daos.DaoUtils.toPutItemRequest;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JoinWithResourceTest extends ResourcesLocalTest {

    @BeforeEach
    public void init() {
        super.init();
    }

    @Test
    public void byResourceIdentifierKeyReturnsDoiRequestWithReferencedResource()
        throws InvalidIssnException, MalformedURLException {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao.getData());
        assertThat(doiRequestDao.getData().getResourceIdentifier(), is(equalTo(resourceDao.getData().getIdentifier())));

        client.putItem(toPutItemRequest(resourceDao));
        client.putItem(toPutItemRequest(doiRequestDao));

        QueryResult result = client.query(new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
            .withKeyConditions(
                resourceDao.byResource(DoiRequestDao.joinByResourceContainedOrderedType(),
                    ResourceDao.joinByResourceContainedOrderedType())
            )
        );

        List<JoinWithResource> retrievedData = parseResult(result);

        DoiRequestDao retrievedDoiRequestDao = (DoiRequestDao) retrievedData.get(0);
        ResourceDao retrievedResourceDao = (ResourceDao) retrievedData.get(1);

        assertThat(retrievedDoiRequestDao, is(equalTo(doiRequestDao)));
        assertThat(retrievedResourceDao, is(equalTo(resourceDao)));
    }

    @Test
    public void byResourceIdentifierKeyReturnsSingleTypeWhenLeftAndRightTypeAreEqual()
        throws InvalidIssnException, MalformedURLException {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao.getData());
        assertThat(doiRequestDao.getData().getResourceIdentifier(), is(equalTo(resourceDao.getData().getIdentifier())));

        client.putItem(toPutItemRequest(resourceDao));
        client.putItem(toPutItemRequest(doiRequestDao));

        QueryRequest query = fetchResourceAndDoiRequest(resourceDao,
            DoiRequestDao.joinByResourceContainedOrderedType());
        QueryResult result = client.query(query);

        List<JoinWithResource> retrievedData = parseResult(result);

        DoiRequestDao retrievedDoiRequestDao = (DoiRequestDao) retrievedData.get(0);

        assertThat(retrievedDoiRequestDao, is(equalTo(doiRequestDao)));
    }
    
    private QueryRequest fetchResourceAndDoiRequest(ResourceDao resourceDao,
                                                    String selectedType

    ) {
        return new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
            .withKeyConditions(
                resourceDao.byResource(selectedType)
            );
    }

    private List<JoinWithResource> parseResult(QueryResult result) {
        return result.getItems()
            .stream()
            .map(item -> parseAttributeValuesMap(item, JoinWithResource.class))
            .collect(Collectors.toList());
    }
}
