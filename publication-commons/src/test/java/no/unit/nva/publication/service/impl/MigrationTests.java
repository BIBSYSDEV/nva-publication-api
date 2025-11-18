package no.unit.nva.publication.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DataCompressor;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.model.utils.CustomerSummary;
import no.unit.nva.publication.service.FakeCristinUnitsUtil;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigrationTests extends ResourcesLocalTest {

    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    protected static final URI CRISTIN_ID = URI.create("https" +
                                                       "://api.dev" +
                                                       ".nva.aws.unit.no/cristin/organization/20754.0.0.0");
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        when(customerService.fetchCustomers()).thenReturn(
            new CustomerList(List.of(new CustomerSummary(randomUri(), CRISTIN_ID))));
        this.resourceService = new ResourceService(client, RESOURCES_TABLE_NAME, Clock.systemDefaultZone(),
                                                   uriRetriever,
                                                   channelClaimClient, customerService, new FakeCristinUnitsUtil());
    }

    @Test
    void shouldMigrateNonTicketDoiRequestsObjectsInTheDatabaseLeavingTheOldObjectInPlace() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        saveFileDirectlyToDatabase("old_doi_request.json");
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(dao -> dao instanceof DoiRequestDao)
                             .map(Dao::getData)
                             .map(entry -> (DoiRequest) entry)
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(2));
    }

    @Test
    void shouldMigrateDoiRequestTicketsWithPublicationDetailsToDoiRequestTicketWithResourceIdentifier() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        saveFileDirectlyToDatabase("ticketentry_doirequest_with_publication_details.json");
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

    @Test
    void shouldMigrateUncompressedDoiRequestToCompressedAndAddNewFields() throws IOException {
        saveFileDirectlyToDatabase("ticketentry_doirequest_with_publication_details.json");
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(dao -> dao instanceof DoiRequestDao)
                             .map(DoiRequestDao.class::cast)
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(1));

        assertThat(doiRequest.getFirst().getIdentifier(), not(nullValue()));
        assertThat(doiRequest.getFirst().getCreatedAt(), not(nullValue()));
        assertThat(doiRequest.getFirst().getModifiedAt(), not(nullValue()));
        assertThat(doiRequest.getFirst().getOwner(), not(nullValue()));
        assertThat(doiRequest.getFirst().getResourceIdentifier(), not(nullValue()));
        assertThat(doiRequest.getFirst().getCustomerId(), not(nullValue()));
        assertThat(doiRequest.getFirst().getTicketIdentifier(), not(nullValue()));

        var dbScan = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiAttributeValue = dbScan.getFirst();
        assertThat(doiAttributeValue.get("data").getB(), not(nullValue()));
        assertThat(doiAttributeValue.get("data").getM(), is(nullValue()));

        var compressedData = doiAttributeValue.get("data").getB().array();
        var decompressedData = new String(DataCompressor.decompress(compressedData), UTF_8);
        var doi = JsonUtils.dtoObjectMapper.readValue(decompressedData, DoiRequest.class);
        assertThat(doi.getIdentifier(), is(equalTo(doiRequest.getFirst().getIdentifier())));
    }

    @Test
    void shouldWorkWithEmptyContributors() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        var publication = createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        publication.getEntityDescription().setContributors(Collections.emptyList());
        updatePublication(publication);
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var resource = getResourceStream(allMigratedItems)
                           .findFirst()
                           .orElseThrow();

        assertThat(resource.getCuratingInstitutions(), hasSize(0));
    }

    @Test
    void shouldWorkWithEmptyEntityDescription() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        var publication = createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        publication.setEntityDescription(null);
        updatePublication(publication);
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var resource = getResourceStream(allMigratedItems)
                           .findFirst()
                           .orElseThrow();

        assertThat(resource.getCuratingInstitutions(), hasSize(0));
    }

    @Test
    void shouldMigrateMainTitleByRemovingWhitespacesAtTheBeginningAndEndOfTheTitle() throws NotFoundException {
        var title = "Some title";
        var trailingSpacesTitle = "  %s  ".formatted(title);
        var publication = randomPublication(Textbook.class);
        publication.getEntityDescription().setMainTitle(trailingSpacesTitle);
        updatePublication(publication);

        migrateResources();

        var migratedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());

        assertEquals(title, migratedResource.getEntityDescription().getMainTitle());
    }

    @Test
    void shouldMigrateMainTitleByRemovingWhitespacesAtTheBeginningAndEndOfTheTitleByKey() throws NotFoundException {
        var title = "Some title";
        var trailingSpacesTitle = "  %s  ".formatted(title);
        var publication = randomPublication(Textbook.class);
        publication.getEntityDescription().setMainTitle(trailingSpacesTitle);
        updatePublication(publication);

        var entries = resourceService.scanResources(1000, START_FROM_BEGINNING, Collections.emptyList())
                          .getDatabaseEntries();
        var key = getFirstKey(entries);

        resourceService.refreshResourcesByKeys(List.of(key));

        var migratedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());

        assertEquals(title, migratedResource.getEntityDescription().getMainTitle());
    }

    private Map<String, AttributeValue> getFirstKey(List<Entity> entries) {
        var key = entries
                      .getFirst()
                      .toDao()
                      .toDynamoFormat();

        key.keySet()
            .removeIf(attributeKey -> !Set.of(PRIMARY_KEY_PARTITION_KEY_NAME, PRIMARY_KEY_SORT_KEY_NAME)
                                           .contains(attributeKey));
        return key;
    }

    private static Stream<Resource> getResourceStream(List<Map<String, AttributeValue>> allMigratedItems) {
        return allMigratedItems.stream()
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                   .filter(ResourceDao.class::isInstance)
                   .map(Dao::getData)
                   .map(Resource.class::cast);
    }

    private void saveFileDirectlyToDatabase(String file) {
        var jsonString = IoUtils.stringFromResources(Path.of("migration", file));
        saveJsonDirectlyToDatabase(jsonString);
    }

    private void saveJsonDirectlyToDatabase(String json) {
        var item = Item.fromJSON(json);
        var itemMap = ItemUtils.toAttributeValues(item);
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(itemMap));
    }

    private Publication createPublicationForOldDoiRequestFormatInResources(SortableIdentifier hardCodedIdentifier) {
        var publication = randomPublication(JournalArticle.class);
        publication.getEntityDescription()
            .getContributors()
            .forEach(contributor ->
                         contributor.getAffiliations()
                             .forEach(affiliation -> ((Organization) affiliation).setId(
                                 URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0")))
            );
        publication.setCuratingInstitutions(null);
        publication.setIdentifier(hardCodedIdentifier);
        updatePublication(publication);

        return publication;
    }

    private void updatePublication(Publication publication) {
        var resource = Resource.fromPublication(publication);
        var dao = resource.toDao();
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(dao.toDynamoFormat()));
    }

    private void migrateResources() {
        var scanResources = resourceService.scanResources(1000, START_FROM_BEGINNING, Collections.emptyList());
        resourceService.refreshResources(scanResources.getDatabaseEntries());
    }
}
