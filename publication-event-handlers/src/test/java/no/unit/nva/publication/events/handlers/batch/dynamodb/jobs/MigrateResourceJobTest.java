package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.model.utils.CustomerSummary;
import no.unit.nva.publication.service.FakeCristinUnitsUtil;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigrateResourceJobTest extends ResourcesLocalTest {

    protected static final URI CRISTIN_ID = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
    private ResourceService resourceService;
    private MigrateResourceJob migrateResourceJob;
    
    @BeforeEach
    void setUp() {
        super.init();
        when(customerService.fetchCustomers()).thenReturn(new CustomerList(List.of(new CustomerSummary(randomUri(), CRISTIN_ID))));
        this.resourceService = new ResourceService(client, RESOURCES_TABLE_NAME, Clock.systemDefaultZone(), uriRetriever,
                                                   channelClaimClient, customerService, new FakeCristinUnitsUtil());
        this.migrateResourceJob = new MigrateResourceJob(resourceService);
    }
    
    @Test
    void shouldMigrateMainTitleByRemovingWhitespacesAtTheBeginningAndEndOfTheTitle() throws NotFoundException {
        var title = "Some title";
        var trailingSpacesTitle = "  %s  ".formatted(title);
        var publication = randomPublication(Textbook.class);
        publication.getEntityDescription().setMainTitle(trailingSpacesTitle);
        
        updatePublication(publication);
        
        var resource = Resource.fromPublication(publication);
        var dao = (ResourceDao) resource.toDao();
        var workItem = createWorkItem(
            dao.getPrimaryKeyPartitionKey(),
            dao.getPrimaryKeySortKey()
        );
        
        migrateResourceJob.executeBatch(List.of(workItem));
        
        var migratedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        assertEquals(title, migratedResource.getEntityDescription().getMainTitle());
    }
    
    @Test
    void shouldHandleMultipleResourcesInBatch() throws NotFoundException {
        var publication1 = randomPublication(Textbook.class);
        publication1.getEntityDescription().setMainTitle("  Title One  ");
        updatePublication(publication1);
        
        var publication2 = randomPublication(Textbook.class);
        publication2.getEntityDescription().setMainTitle("  Title Two  ");
        updatePublication(publication2);
        
        var publication3 = randomPublication(Textbook.class);
        publication3.getEntityDescription().setMainTitle("  Title Three  ");
        updatePublication(publication3);
        
        var workItems = List.of(
            createWorkItemForPublication(publication1),
            createWorkItemForPublication(publication2),
            createWorkItemForPublication(publication3)
        );
        
        migrateResourceJob.executeBatch(workItems);
        
        var migrated1 = resourceService.getResourceByIdentifier(publication1.getIdentifier());
        var migrated2 = resourceService.getResourceByIdentifier(publication2.getIdentifier());
        var migrated3 = resourceService.getResourceByIdentifier(publication3.getIdentifier());
        
        assertEquals("Title One", migrated1.getEntityDescription().getMainTitle());
        assertEquals("Title Two", migrated2.getEntityDescription().getMainTitle());
        assertEquals("Title Three", migrated3.getEntityDescription().getMainTitle());
    }
    
    @Test
    void shouldHandleEmptyBatch() {
        migrateResourceJob.executeBatch(List.of());
        
        assertNotNull(migrateResourceJob);
    }
    
    @Test
    void shouldReturnCorrectJobType() {
        assertEquals("MIGRATE_RESOURCE", migrateResourceJob.getJobType());
    }
    
    private void updatePublication(Publication publication) {
        var resource = Resource.fromPublication(publication);
        var dao = resource.toDao();
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(dao.toDynamoFormat()));
    }
    
    private BatchWorkItem createWorkItem(String partitionKey, String sortKey) {
        var key = new DynamodbResourceBatchDynamoDbKey(partitionKey, sortKey);
        return new BatchWorkItem(key, "MIGRATE_RESOURCE");
    }
    
    private BatchWorkItem createWorkItemForPublication(Publication publication) {
        var resource = Resource.fromPublication(publication);
        var dao = (ResourceDao) resource.toDao();
        return createWorkItem(dao.getPrimaryKeyPartitionKey(), dao.getPrimaryKeySortKey());
    }
}