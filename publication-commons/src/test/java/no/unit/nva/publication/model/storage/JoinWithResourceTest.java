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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
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

        var result = client.query(QueryRequest.builder()
                                       .tableName(RESOURCES_TABLE_NAME)
                                       .indexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                                       .keyConditions(resourceDao.byResource(
                                           resourceDao.joinByResourceContainedOrderedType(),
                                           doiRequestDao.joinByResourceContainedOrderedType()))
                                       .build());

        var retrievedData = parseResult(result);

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

        var query = fetchResourceAndDoiRequest(resourceDao,
                                              doiRequestDao.joinByResourceContainedOrderedType());
        var result = client.query(query);

        var retrievedData = parseResult(result);

        DoiRequestDao retrievedDoiRequestDao = (DoiRequestDao) retrievedData.get(0);

        assertThat(retrievedDoiRequestDao, is(equalTo(doiRequestDao)));
    }

    private QueryRequest fetchResourceAndDoiRequest(ResourceDao resourceDao, String selectedType) {
        return QueryRequest.builder()
                   .tableName(RESOURCES_TABLE_NAME)
                   .indexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .keyConditions(resourceDao.byResource(selectedType))
                   .build();
    }

    private List<JoinWithResource> parseResult(QueryResponse result) {
        return result.items()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, JoinWithResource.class))
                   .collect(Collectors.toList());
    }
}
