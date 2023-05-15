package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigrationTests extends ResourcesLocalTest {

    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = new ResourceService(client, CLOCK);
    }

    @Test
    void shouldWriteBackEntryAsIsWhenMigrating() throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        var savedPublication = resourceService.insertPreexistingPublication(publication);
        migrateResources();

        var migratedResource = resourceService.getResourceByIdentifier(savedPublication.getIdentifier());
        var migratedPublication = migratedResource.toPublication();
        assertThat(migratedPublication, is(equalTo(publication)));
    }

    @Test
    void shouldMigrateDoiRequestsToTickets() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        saveOlDoiRequestDirectlyInDatabase();
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(dao -> dao instanceof DoiRequestDao)
                             .map(Dao::getData)
                             .map(entry -> (DoiRequest) entry)
                             .filter(entry -> nonNull(entry.getResourceIdentifier()))
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(1));
    }

    private void saveOlDoiRequestDirectlyInDatabase() {
        var jsonString = IoUtils.stringFromResources(Path.of("migration", "old_doi_request.json"));
        var item = Item.fromJSON(jsonString);
        var itemMap = ItemUtils.toAttributeValues(item);
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(itemMap));
    }

    private void createPublicationForOldDoiRequestFormatInResources(SortableIdentifier hardCodedIdentifier) {
        var publication = randomPublication();
        publication.setIdentifier(hardCodedIdentifier);
        var resource = Resource.fromPublication(publication);
        var dao = resource.toDao();
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(dao.toDynamoFormat()));
    }

    private void migrateResources() {
        var scanResources = resourceService.scanResources(1000, START_FROM_BEGINNING);
        resourceService.refreshResources(scanResources.getDatabaseEntries());
    }
}
