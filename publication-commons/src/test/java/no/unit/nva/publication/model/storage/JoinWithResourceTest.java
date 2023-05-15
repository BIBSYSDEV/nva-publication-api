package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.DaoUtils.doiRequestDao;
import static no.unit.nva.publication.model.storage.DaoUtils.sampleResourceDao;
import static no.unit.nva.publication.model.storage.DaoUtils.toPutItemRequest;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.model.storage.JoinWithResource.Constants.DOI_REQUEST_INDEX_IN_QUERY_RESULT;
import static no.unit.nva.publication.model.storage.JoinWithResource.Constants.RESOURCE_INDEX_IN_QUERY_RESULT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoinWithResourceTest extends ResourcesLocalTest {

    @BeforeEach
    public void init() {
        super.init();
    }

    @Test
    void byResourceIdentifierKeyReturnsDoiRequestWithReferencedResource() {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao);
        assertThat(doiRequestDao.getTicketEntry().getResourceIdentifier(),
                   is(equalTo(resourceDao.getData().getIdentifier())));

        client.putItem(toPutItemRequest(resourceDao));
        client.putItem(toPutItemRequest(doiRequestDao));

        QueryResult result = client.query(new QueryRequest().withTableName(RESOURCES_TABLE_NAME)
                                              .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                                              .withKeyConditions(resourceDao.byResource(
                                                  resourceDao.joinByResourceContainedOrderedType(),
                                                  doiRequestDao.joinByResourceContainedOrderedType())));

        List<JoinWithResource> retrievedData = parseResult(result);

        var retrievedDoiRequestDao = (DoiRequestDao) retrievedData.get(DOI_REQUEST_INDEX_IN_QUERY_RESULT);
        var retrievedResourceDao = (ResourceDao) retrievedData.get(RESOURCE_INDEX_IN_QUERY_RESULT);

        assertThat(retrievedDoiRequestDao, is(equalTo(doiRequestDao)));
        assertThat(retrievedResourceDao, is(equalTo(resourceDao)));
    }

    @Test
    void byResourceIdentifierKeyReturnsSingleTypeWhenLeftAndRightTypeAreEqual() {
        ResourceDao resourceDao = sampleResourceDao();
        DoiRequestDao doiRequestDao = doiRequestDao(resourceDao);
        assertThat(doiRequestDao.getTicketEntry().getResourceIdentifier(),
                   is(equalTo(resourceDao.getData().getIdentifier())));

        client.putItem(toPutItemRequest(resourceDao));
        client.putItem(toPutItemRequest(doiRequestDao));

        QueryRequest query = fetchResourceAndDoiRequest(resourceDao,
                                                        doiRequestDao.joinByResourceContainedOrderedType());
        QueryResult result = client.query(query);

        List<JoinWithResource> retrievedData = parseResult(result);

        DoiRequestDao retrievedDoiRequestDao = (DoiRequestDao) retrievedData.get(0);

        assertThat(retrievedDoiRequestDao, is(equalTo(doiRequestDao)));
    }

    private QueryRequest fetchResourceAndDoiRequest(ResourceDao resourceDao, String selectedType

    ) {
        return new QueryRequest().withTableName(RESOURCES_TABLE_NAME)
                   .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .withKeyConditions(resourceDao.byResource(selectedType));
    }

    private List<JoinWithResource> parseResult(QueryResult result) {
        return result.getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, JoinWithResource.class))
                   .collect(Collectors.toList());
    }
}
