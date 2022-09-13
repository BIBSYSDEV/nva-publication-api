package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.storage.DaoUtils.sampleResourceDao;
import static no.unit.nva.publication.model.storage.DaoUtils.toPutItemRequest;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.SingletonCollector;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceDaoTest extends ResourcesLocalTest {
    
    public static final String SAMPLE_USER = "some@owner";
    public static final SortableIdentifier SAMPLE_IDENTIFIER = SortableIdentifier.next();
    public static final String PUBLISHER_IDENTIFIER = "publisherIdentifier";
    public static final URI SAMPLE_PUBLISHER = URI.create("https://some.example.org/" + PUBLISHER_IDENTIFIER);
    public static final UserInstance SAMPLE_USER_INSTANCE = UserInstance.create(SAMPLE_USER, SAMPLE_PUBLISHER);
    
    @BeforeEach
    public void init() {
        super.init();
    }
    
    @Test
    void queryObjectReturnsObjectWithNonNullPrimaryPartitionKey() {
        ResourceDao queryObject = ResourceDao.queryObject(SAMPLE_USER_INSTANCE, SAMPLE_IDENTIFIER);
        assertThat(queryObject.getPrimaryKeyPartitionKey(), containsString(PUBLISHER_IDENTIFIER));
        assertThat(queryObject.getPrimaryKeyPartitionKey(), containsString(SAMPLE_USER));
    }
    
    @Test
    void queryObjectReturnsObjectWithNonNullPrimarySortKey() {
        ResourceDao queryObject = ResourceDao.queryObject(SAMPLE_USER_INSTANCE, SAMPLE_IDENTIFIER);
        assertThat(queryObject.getPrimaryKeySortKey(), containsString(SAMPLE_IDENTIFIER.toString()));
    }
    
    @Test
    void constructPrimaryPartitionKeyReturnsStringContainingTypePublisherAndOwner() {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication);
        var dao = new ResourceDao(resource);
        
        String primaryPartitionKey = ResourceDao.constructPrimaryPartitionKey(SAMPLE_PUBLISHER, SAMPLE_USER);
        String expectedKey = dao.indexingType()
                             + KEY_FIELDS_DELIMITER
                             + PUBLISHER_IDENTIFIER
                             + KEY_FIELDS_DELIMITER
                             + SAMPLE_USER;
        assertThat(primaryPartitionKey, is(equalTo(expectedKey)));
    }
    
    @Test
    void getResourceByCristinIdPartitionKeyReturnsANullValueWhenObjectHasNoCristinIdentifier() {
        ResourceDao daoWithoutCristinId = createResourceDaoWithoutCristinIdentifier();
        assertThat(daoWithoutCristinId.getResourceByCristinIdentifierPartitionKey(),
            is(equalTo(null)));
    }
    

    
    @Test
    void dynamoClientReturnsResourceWithMatchingCristinIdWhenSearchingResourcesByCristinId() {
        var dao = sampleResourceDao();
        client.putItem(toPutItemRequest(dao));
        var actualResult = queryDbFindByCristinIdentifier(dao);
        assertThat(actualResult, Matchers.is(Matchers.equalTo(dao)));
    }
    
    @Test
    void dynamoClientReturnsOnlyResourcesWithCristinIdWhenSearchingResourcesByCristinId() {
        var daoWithCristinId = sampleResourceDao();
        var daoWithoutCristinId = createResourceDaoWithoutCristinIdentifier();
        client.putItem(toPutItemRequest(daoWithCristinId));
        client.putItem(toPutItemRequest(daoWithoutCristinId));
        var result = client.scan(
            new ScanRequest()
                .withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                .withIndexName(RESOURCE_BY_CRISTIN_ID_INDEX_NAME));
        assertThat(result.getCount(), Matchers.is(Matchers.equalTo(1)));
    }
    
    protected static ResourceDao createResourceDaoWithoutCristinIdentifier() {
        return new ResourceDao(sampleResourceDao()
                                   .getResource()
                                   .copy()
                                   .withAdditionalIdentifiers(null)
                                   .build());
    }
    
    private ResourceDao queryDbFindByCristinIdentifier(ResourceDao dao) {
        var queryRequest = dao.createQueryFindByCristinIdentifier();
        return client.query(queryRequest)
                   .getItems()
                   .stream()
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, dao.getClass()))
                   .collect(SingletonCollector.collectOrElse(null));
    }
}