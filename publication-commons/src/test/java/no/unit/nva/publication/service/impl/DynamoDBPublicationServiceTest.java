package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.DynamoDBException;
import no.unit.nva.publication.exception.InputException;
import no.unit.nva.publication.exception.NotFoundException;
import no.unit.nva.publication.exception.NotImplementedException;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.PublishPublicationStatus;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.ERROR_MAPPING_ITEM_TO_PUBLICATION;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.ERROR_READING_FROM_TABLE;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.ERROR_WRITING_TO_TABLE;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.PUBLICATION_NOT_FOUND;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.PUBLISH_COMPLETED;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.PUBLISH_IN_PROGRESS;
import static no.unit.nva.publication.service.impl.PublicationsDynamoDBLocal.BY_PUBLISHER_INDEX_NAME;
import static no.unit.nva.publication.service.impl.PublicationsDynamoDBLocal.NVA_RESOURCES_TABLE_NAME;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class DynamoDBPublicationServiceTest {

    private static final UUID ID1 = UUID.randomUUID();
    private static final  UUID ID2 = UUID.randomUUID();

    private static final Instant INSTANT1 = Instant.now();
    private static final Instant INSTANT2 = INSTANT1.plusSeconds(100);
    private static final Instant INSTANT3 = INSTANT2.plusSeconds(100);
    private static final Instant INSTANT4 = INSTANT3.plusSeconds(100);

    public static final String OWNER = "owner@example.org";
    public static final URI PUBLISHER_ID = URI.create("http://example.org/123");
    public static final String TABLE_NAME_ENV = "TABLE_NAME";
    public static final String BY_PUBLISHER_INDEX_NAME_ENV = "BY_PUBLISHER_INDEX_NAME";
    public static final String INVALID_JSON = "{\"test\" = \"invalid json }";

    @Rule
    public PublicationsDynamoDBLocal db =  new PublicationsDynamoDBLocal();

    private DynamoDBPublicationService publicationService;
    private Environment environment;
    private AmazonDynamoDB client;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        client = DynamoDBEmbedded.create().amazonDynamoDB();
        environment = mock(Environment.class);
        publicationService = new DynamoDBPublicationService(
                objectMapper,
                db.getTable(),
                db.getByPublisherIndex()
        );
    }

    @Test
    @DisplayName("notImplemented Methods Throws NotImplementedException")
    public void notImplementedMethodsThrowsNotImplementedException() {
        assertThrows(NotImplementedException.class, () ->  {
            publicationService.getPublicationsByPublisher(null);
        });
    }

    @Test
    @DisplayName("calling Constructor When Missing Env Throws Exception")
    public void callingConstructorWhenMissingEnvThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new DynamoDBPublicationService(client, objectMapper, environment)
        );
    }

    @Test
    @DisplayName("missing Table Env")
    public void missingTableEnv() {
        when(environment.readEnv(TABLE_NAME_ENV)).thenReturn(NVA_RESOURCES_TABLE_NAME);
        assertThrows(IllegalArgumentException.class,
            () -> new DynamoDBPublicationService(client, objectMapper, environment)
        );
    }

    @Test
    @DisplayName("missing Index Env")
    public void missingIndexEnv() {
        when(environment.readEnv(BY_PUBLISHER_INDEX_NAME_ENV)).thenReturn(BY_PUBLISHER_INDEX_NAME);
        assertThrows(IllegalArgumentException.class,
            () -> new DynamoDBPublicationService(client, objectMapper, environment)
        );
    }

    @Test
    @DisplayName("calling Constructor With All Env")
    public void callingConstructorWithAllEnv() {
        Environment environment = Mockito.mock(Environment.class);
        when(environment.readEnv(DynamoDBPublicationService.TABLE_NAME_ENV)).thenReturn(TABLE_NAME_ENV);
        when(environment.readEnv(DynamoDBPublicationService.BY_PUBLISHER_INDEX_NAME_ENV))
                .thenReturn(BY_PUBLISHER_INDEX_NAME);
        new DynamoDBPublicationService(client, objectMapper, environment);
    }

    @Test
    public void getPublicationReturnsExistingPublicationWhenInputIsExistingIdentifier() throws Exception {
        Publication storedPublication = publication();
        publicationService.createPublication(storedPublication);
        Publication retrievedPublication = publicationService.getPublication(storedPublication.getIdentifier());
        assertEquals(retrievedPublication, storedPublication);
    }

    @Test
    public void getPublicationOnEmptyTableThrowsNotFoundException() {
        UUID nonExistingIdentifier = UUID.randomUUID();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> publicationService.getPublication(
                 nonExistingIdentifier));
        assertEquals(PUBLICATION_NOT_FOUND + nonExistingIdentifier, exception.getMessage());

    }

    @Test
    public void updateExistingCustomerWithNewOwner() throws Exception {
        String newOwner = "New Owner";
        Publication publication = publication();
        publicationService.createPublication(publication);

        publication.setOwner(newOwner);
        Publication updatedPublication = publicationService.updatePublication(
                publication.getIdentifier(), publication);
        assertEquals(newOwner, updatedPublication.getOwner());
    }

    @Test
    public void updateExistingCustomerChangesModifiedDate() throws Exception {
        Publication publication = publication();
        Publication createdPublication = publicationService.createPublication(publication);

        Publication updatedPublication = publicationService.updatePublication(
                publication.getIdentifier(), publication);
        assertNotEquals(createdPublication.getModifiedDate(), updatedPublication.getModifiedDate());
    }

    @Test
    public void updateExistingCustomerPreservesCreatedDate() throws Exception {
        Publication publication = publication();
        publicationService.createPublication(publication);

        Publication updatedPublication = publicationService.updatePublication(
                publication.getIdentifier(), publication);
        assertEquals(publication.getCreatedDate(), updatedPublication.getCreatedDate());
    }

    @Test
    public void updateExistingCustomerWithDifferentIdentifiersThrowsException() throws Exception {
        Publication publication = publication();
        publicationService.createPublication(publication);
        UUID differentIdentifier = UUID.randomUUID();

        InputException exception = assertThrows(InputException.class,
            () -> publicationService.updatePublication(differentIdentifier, publication));
        String expectedMessage = String.format(DynamoDBPublicationService.IDENTIFIERS_NOT_EQUAL,
                differentIdentifier, publication.getIdentifier());
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    @DisplayName("empty Table Returns No Publications")
    public void emptyTableReturnsNoPublications() throws ApiGatewayException {
        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
                OWNER,
                PUBLISHER_ID);

        assertEquals(0, publications.size());
    }

    @Test
    @DisplayName("nonEmpty Table Returns Publications")
    public void nonEmptyTableReturnsPublications() throws ApiGatewayException {
        Publication publication = publication();
        publicationService.createPublication(publication);

        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
                OWNER,
                PUBLISHER_ID);

        assertEquals(1, publications.size());
        assertEquals(publication.getEntityDescription().getMainTitle(), publications.get(0).getMainTitle());
        assertEquals(publication.getOwner(), publications.get(0).getOwner());

    }

    @Test
    @DisplayName("invalid Item Json")
    public void invalidItemJson() {
        Item item = mock(Item.class);
        when(item.toJSON()).thenReturn(INVALID_JSON);
        Optional<PublicationSummary> publicationSummary = publicationService.toPublicationSummary(item);
        Assertions.assertTrue(publicationSummary.isEmpty());
    }

    @Test
    @DisplayName("filterOutOlderVersionsOfPublications returns only the latest version of each publication")
    public void filterOutOlderVersionsOfPublicationsReturnsOnlyTheLatestVersionOfEachPublication() {
        List<PublicationSummary> publications = publicationSummariesWithDuplicateUuuIds();
        ArrayList<PublicationSummary> expected = new ArrayList<>();
        expected.add(createPublication(ID1, INSTANT2));
        expected.add(createPublication(ID2, INSTANT4));
        List<PublicationSummary> actual = DynamoDBPublicationService.filterOutOlderVersionsOfPublications(publications);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
        assertThat(expected, containsInAnyOrder(actual.toArray()));
    }

    @Test
    @DisplayName("filterOutOlderVersionsOfPublications returns only the single version of a publication")
    public void filterOutOlderVersionsOfPublicationsReturnsTheSingleVersionOfAPublication() {
        List<PublicationSummary> publications = publicationSummariesWithoutDuplicateUuIds();
        ArrayList<PublicationSummary> expected = new ArrayList<>();
        expected.add(createPublication(ID1, INSTANT1));
        expected.add(createPublication(ID2, INSTANT3));
        List<PublicationSummary> actual = DynamoDBPublicationService.filterOutOlderVersionsOfPublications(publications);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
        assertThat(expected, containsInAnyOrder(actual.toArray()));
    }

    @Test
    public void createPublicationTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        Index index = mock(Index.class);
        when(failingTable.putItem(any(Item.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService = new DynamoDBPublicationService(
                objectMapper,
                failingTable,
                index
        );
        DynamoDBException exception = assertThrows(DynamoDBException.class,
            () -> failingService.createPublication(publication()));
        assertEquals(ERROR_WRITING_TO_TABLE, exception.getMessage());
    }

    @Test
    public void getPublicationTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        Index index = mock(Index.class);
        when(failingTable.query(any(QuerySpec.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService = new DynamoDBPublicationService(
                objectMapper,
                failingTable,
                index
        );
        DynamoDBException exception = assertThrows(DynamoDBException.class,
            () -> failingService.getPublication(UUID.randomUUID()));
        assertEquals(ERROR_READING_FROM_TABLE, exception.getMessage());
    }

    @Test
    public void getPublicationsByOwnerTableErrorThrowsException() {
        Table table = mock(Table.class);
        Index failingIdex = mock(Index.class);
        when(failingIdex.query(any(QuerySpec.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService = new DynamoDBPublicationService(
                objectMapper,
                table,
                failingIdex
        );
        DynamoDBException exception = assertThrows(DynamoDBException.class,
            () -> failingService.getPublicationsByOwner(OWNER, PUBLISHER_ID));
        assertEquals(ERROR_READING_FROM_TABLE, exception.getMessage());
    }

    @Test
    public void updatePublicationTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        Index index = mock(Index.class);
        when(failingTable.putItem(any(Item.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService = new DynamoDBPublicationService(
                objectMapper,
                failingTable,
                index
        );
        Publication publication = publication();
        publication.setIdentifier(UUID.randomUUID());
        DynamoDBException exception = assertThrows(DynamoDBException.class,
            () -> failingService.updatePublication(publication.getIdentifier(), publication));
        assertEquals(ERROR_WRITING_TO_TABLE, exception.getMessage());
    }

    @Test
    public void publicationToItemThrowsExceptionWhenInvalidJson() throws JsonProcessingException {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(any(Publication.class))).thenThrow(JsonProcessingException.class);
        DynamoDBPublicationService failingService = new DynamoDBPublicationService(
                failingObjectMapper,
                db.getTable(),
                db.getByPublisherIndex()
        );
        InputException exception = assertThrows(InputException.class,
            () -> failingService.publicationToItem(publication()));
        assertEquals(DynamoDBPublicationService.ERROR_MAPPING_PUBLICATION_TO_ITEM, exception.getMessage());
    }

    @Test
    public void itemToPublicationThrowsExceptionWhenInvalidJson() {
        Item item = mock(Item.class);
        when(item.toJSON()).thenThrow(new IllegalStateException());
        DynamoDBException exception = assertThrows(DynamoDBException.class,
            () -> publicationService.itemToPublication(item));
        assertEquals(ERROR_MAPPING_ITEM_TO_PUBLICATION, exception.getMessage());

    }

    @Test
    public void canPublishPublicationReturnsAccepted() throws Exception {
        Publication publicationToPublish = publicationService.createPublication(publication());

        PublishPublicationStatus actual = publicationService.publishPublication(publicationToPublish.getIdentifier());

        PublishPublicationStatus expected = new PublishPublicationStatus(PUBLISH_IN_PROGRESS, SC_ACCEPTED);
        assertEquals(expected, actual);
    }

    @Test
    public void publicationAlreadyPublishedReturnsNoContent() throws Exception {
        Publication publicationToPublish = publicationService.createPublication(publication());

        // publish
        publicationService.publishPublication(publicationToPublish.getIdentifier());
        // trying to publish again
        PublishPublicationStatus actual = publicationService.publishPublication(publicationToPublish.getIdentifier());

        PublishPublicationStatus expected = new PublishPublicationStatus(PUBLISH_COMPLETED, SC_NO_CONTENT);
        assertEquals(expected, actual);
    }

    private List<PublicationSummary> publicationSummariesWithDuplicateUuuIds() {
        List<PublicationSummary> publicationSummaries = new ArrayList<>();

        publicationSummaries.add(createPublication(ID1, INSTANT1));
        publicationSummaries.add(createPublication(ID1, INSTANT2));
        publicationSummaries.add(createPublication(ID2, INSTANT3));
        publicationSummaries.add(createPublication(ID2, INSTANT4));

        return publicationSummaries;
    }

    private List<PublicationSummary> publicationSummariesWithoutDuplicateUuIds() {
        List<PublicationSummary> publicationSummaries = new ArrayList<>();
        publicationSummaries.add(createPublication(ID1, INSTANT1));
        publicationSummaries.add(createPublication(ID2, INSTANT3));
        return publicationSummaries;
    }

    private PublicationSummary createPublication(UUID id, Instant modifiedDate) {
        return new PublicationSummary.Builder()
            .withIdentifier(id)
            .withModifiedDate(modifiedDate)
            .withCreatedDate(INSTANT1)
            .withOwner("junit")
            .withMainTitle("Some main title")
            .withStatus(DRAFT)
            .build();
    }

    private Publication publication() {
        Instant oneMinuteInThePast = Instant.now().minusSeconds(60L);
        return new Publication.Builder()
                .withIdentifier(UUID.randomUUID())
                .withCreatedDate(oneMinuteInThePast)
                .withModifiedDate(oneMinuteInThePast)
                .withOwner(OWNER)
                .withStatus(PublicationStatus.DRAFT)
                .withPublisher(new Organization.Builder().withId(PUBLISHER_ID).build())
                .withEntityDescription(new EntityDescription.Builder().withMainTitle("DynamoDB Local Testing").build())
                .build();
    }
}