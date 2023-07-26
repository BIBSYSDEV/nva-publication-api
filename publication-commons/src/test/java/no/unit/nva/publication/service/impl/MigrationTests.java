package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Contribution;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.ContributionDao;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.hamcrest.HamcrestArgumentMatcher;

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
    void shouldMigrateNonTicketDoiRequestsObjectsInTheDatabaseLeavingTheOldObjectInPlace() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        saveOlDoiRequestDirectlyInDatabase("old_doi_request.json");
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(DoiRequestDao.class::isInstance)
                             .map(Dao::getData)
                             .map(entry -> (DoiRequest) entry)
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(2));
    }

    @Test
    void shouldMigrateDoiRequestTicketsWithPublicationDetailsToDoiRequestTicketWithResourceIdentifier() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        saveOlDoiRequestDirectlyInDatabase("ticketentry_doirequest_with_publication_details.json");
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(DoiRequestDao.class::isInstance)
                             .map(Dao::getData)
                             .map(entry -> (DoiRequest) entry)
                             .filter(entry -> nonNull(entry.getResourceIdentifier()))
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(1));
    }

    @Test
    void shouldMigrateResourceWithContributionToSeperateEntries() {
        var contributions = List.of(
            randomContributor(),
            randomContributor()
        );
        var publication = randomPublication().copy().withEntityDescription(
            new EntityDescription.Builder().withContributors(
                contributions
            ).build()
        ).build();
        saveResourceDirectlyToDatabase(Resource.fromPublication(publication));

        migrateResources();

        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var daos = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                       .collect(Collectors.toList());

        assertThat(daos.size(), is(equalTo(3)));
        assertThat(daos.stream().filter(ResourceDao.class::isInstance).count(), is(equalTo(1L)));
        assertThat(daos.stream().filter(ContributionDao.class::isInstance).count(), is(equalTo(2L)));

        var fetchedResource = daos.stream().filter(ResourceDao.class::isInstance)
                                  .map(Dao::getData)
                                  .map(Resource.class::cast).findFirst().get();
        var fetchedContributors = daos
                                      .stream()
                                      .filter(ContributionDao.class::isInstance)
                                      .map(Dao::getData)
                                      .map(Contribution.class::cast)
                                      .map(Contribution::getContributor)
                                      .collect(Collectors.toList());

        assertThat(fetchedResource.getEntityDescription().getContributors().size(), is(equalTo(0)));
        assertTrue(fetchedContributors.get(0).equals(contributions.get(0)) || fetchedContributors.get(0).equals(contributions.get(1)));
        assertTrue(fetchedContributors.get(1).equals(contributions.get(0)) || fetchedContributors.get(1).equals(contributions.get(1)));
    }

    @Test
    void shouldNotMigrateResourceThatHasBeenMigrated() {
        var contributions = List.of(
            randomContributor(),
            randomContributor()
        );
        var publication = randomPublication().copy().withEntityDescription(
            new EntityDescription.Builder().withContributors(
                List.of()
            ).build()
        ).build();

        saveResourceWithContributionsDirectlyToDatabase(Resource.fromPublication(publication), contributions);

        migrateResources();

        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var daos = allMigratedItems.stream()
                       .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                       .collect(Collectors.toList());

        assertThat(daos.size(), is(equalTo(3)));
        assertThat(daos.stream().filter(ResourceDao.class::isInstance).count(), is(equalTo(1L)));
        assertThat(daos.stream().filter(ContributionDao.class::isInstance).count(), is(equalTo(2L)));

        var fetchedResource = daos.stream().filter(ResourceDao.class::isInstance)
                                  .map(Dao::getData)
                                  .map(Resource.class::cast).findFirst().get();
        var fetchedContributors = daos
                                      .stream()
                                      .filter(ContributionDao.class::isInstance)
                                      .map(Dao::getData)
                                      .map(Contribution.class::cast)
                                      .map(Contribution::getContributor)
                                      .collect(Collectors.toList());

        assertThat(fetchedResource.getEntityDescription().getContributors().size(), is(equalTo(0)));
        assertTrue(fetchedContributors.get(0).equals(contributions.get(0)) || fetchedContributors.get(0).equals(contributions.get(1)));
        assertTrue(fetchedContributors.get(1).equals(contributions.get(0)) || fetchedContributors.get(1).equals(contributions.get(1)));
    }

    private void saveOlDoiRequestDirectlyInDatabase(String file) {
        var jsonString = IoUtils.stringFromResources(Path.of("migration", file));
        var item = Item.fromJSON(jsonString);
        var itemMap = ItemUtils.toAttributeValues(item);
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(itemMap));
    }

    private void saveResourceDirectlyToDatabase(Resource resource) {
        var itemMap = new ResourceDao(resource).toDynamoFormat();
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(itemMap));
    }

    private void saveResourceWithContributionsDirectlyToDatabase(Resource resource, List<Contributor> contributions) {
        var itemMap = new ResourceDao(resource).toDynamoFormat();
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(itemMap));
        contributions.stream()
            .map(c -> new ContributionDao(resource, c))
            .map(Dao::toDynamoFormat)
            .forEach(c -> client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(c)));
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

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }
}
