package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.publication.PublicationGenerator.OWNER;
import static no.unit.nva.publication.PublicationGenerator.PUBLISHER_ID;
import static no.unit.nva.publication.PublicationGenerator.publicationWithIdentifier;
import static no.unit.nva.publication.PublicationGenerator.publicationWithoutIdentifier;
import static no.unit.nva.publication.exception.InvalidPublicationException.ERROR_MESSAGE_TEMPLATE;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.ERROR_MAPPING_ITEM_TO_PUBLICATION;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.ERROR_READING_FROM_TABLE;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.ERROR_WRITING_TO_TABLE;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.PUBLICATION_NOT_FOUND;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.PUBLISH_COMPLETED;
import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.PUBLISH_IN_PROGRESS;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.DynamoDBException;
import no.unit.nva.publication.exception.InputException;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.NotFoundException;
import no.unit.nva.publication.exception.NotImplementedException;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.PublicationsDynamoDBLocal;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;

@EnableRuleMigrationSupport
class DynamoDBPublicationServiceTest {

    public static final String TABLE_NAME_ENV = "TABLE_NAME";
    public static final String BY_PUBLISHER_INDEX_NAME_ENV = "BY_PUBLISHER_INDEX_NAME";
    public static final String INVALID_JSON = "{\"test\" = \"invalid json }";
    public static final String BY_PUBLISHED_PUBLICATIONS_INDEX_NAME = "BY_PUBLISHED_PUBLICATIONS_INDEX_NAME";
    private static final UUID ID1 = UUID.randomUUID();
    private static final UUID ID2 = UUID.randomUUID();
    private static final Instant INSTANT1 = Instant.now();
    private static final Instant INSTANT2 = INSTANT1.plusSeconds(100);
    private static final Instant INSTANT3 = INSTANT2.plusSeconds(100);
    private static final Instant INSTANT4 = INSTANT3.plusSeconds(100);
    @Rule
    public PublicationsDynamoDBLocal db = new PublicationsDynamoDBLocal();

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
            db.getByPublisherIndex(),
            db.getByPublishedDateIndex()
        );
    }

    @Test
    @DisplayName("notImplemented Methods Throws NotImplementedException")
    public void notImplementedMethodsThrowsNotImplementedException() {
        assertThrows(NotImplementedException.class, () -> publicationService
            .getPublicationsByPublisher(null));
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
        when(environment.readEnv(TABLE_NAME_ENV)).thenReturn(PublicationsDynamoDBLocal.NVA_RESOURCES_TABLE_NAME);
        assertThrows(IllegalArgumentException.class,
            () -> new DynamoDBPublicationService(client, objectMapper, environment)
        );
    }

    @Test
    @DisplayName("missing Index Env")
    public void missingIndexEnv() {
        when(environment.readEnv(BY_PUBLISHER_INDEX_NAME_ENV))
            .thenReturn(PublicationsDynamoDBLocal.BY_PUBLISHER_INDEX_NAME);
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
            .thenReturn(PublicationsDynamoDBLocal.BY_PUBLISHER_INDEX_NAME);
        when(environment.readEnv(DynamoDBPublicationService.BY_PUBLISHED_PUBLICATIONS_INDEX_NAME))
            .thenReturn(BY_PUBLISHED_PUBLICATIONS_INDEX_NAME);
        new DynamoDBPublicationService(client, objectMapper, environment);
    }

    //DONE
    @Test
    public void getPublicationReturnsExistingPublicationWhenInputIsExistingIdentifier() throws Exception {
        Publication storedPublication = publicationWithIdentifier();
        publicationService.createPublication(storedPublication);
        Publication retrievedPublication = publicationService.getPublication(storedPublication.getIdentifier());
        assertEquals(retrievedPublication, storedPublication);
    }

    //DONE
    @Test
    public void getPublicationOnEmptyTableThrowsNotFoundException() {
        SortableIdentifier nonExistingIdentifier = SortableIdentifier.next();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> publicationService.getPublication(
            nonExistingIdentifier));
        assertEquals(PUBLICATION_NOT_FOUND + nonExistingIdentifier, exception.getMessage());
    }

    //DONE
    @Test
    public void updateExistingCustomerWithNewOwner() throws Exception {
        String newOwner = "New Owner";
        Publication publication = publicationWithIdentifier();
        publicationService.createPublication(publication);

        publication.setOwner(newOwner);
        Publication updatedPublication = publicationService.updatePublication(
            publication.getIdentifier(), publication);
        assertEquals(newOwner, updatedPublication.getOwner());
    }

    //DONE
    @Test
    public void updateExistingCustomerChangesModifiedDate() throws Exception {
        Publication publication = publicationWithIdentifier();
        Publication createdPublication = publicationService.createPublication(publication);
        Instant initialInstant = createdPublication.getModifiedDate();
        Publication updatedPublication = publicationService.updatePublication(
            publication.getIdentifier(), publication);
        Instant updatedInstant = updatedPublication.getModifiedDate();
        assertNotEquals(initialInstant, updatedInstant);
    }

    //DONE
    @Test
    public void updateExistingCustomerPreservesCreatedDate() throws Exception {
        Publication publication = publicationWithIdentifier();
        publicationService.createPublication(publication);

        Publication updatedPublication = publicationService.updatePublication(
            publication.getIdentifier(), publication);
        assertEquals(publication.getCreatedDate(), updatedPublication.getCreatedDate());
    }

    //DONE
    @Test
    public void updateExistingCustomerWithDifferentIdentifiersThrowsException() throws Exception {
        Publication publication = publicationWithIdentifier();
        publicationService.createPublication(publication);
        SortableIdentifier differentIdentifier = SortableIdentifier.next();
        Executable executable = () -> publicationService.updatePublication(differentIdentifier, publication);
        InputException exception = assertThrows(InputException.class, executable);
        String expectedMessage = String.format(DynamoDBPublicationService.IDENTIFIERS_NOT_EQUAL,
            differentIdentifier, publication.getIdentifier());
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    //DONE
    @DisplayName("empty Table Returns No Publications")
    public void emptyTableReturnsNoPublications() throws ApiGatewayException {
        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
            OWNER,
            URI.create(PUBLISHER_ID));

        assertEquals(0, publications.size());
    }

    //DONE
    @Test
    @DisplayName("empty Table Returns No Published Publications")
    public void emptyTableReturnsNoPublishedPublications() throws ApiGatewayException {
        List<PublicationSummary> publications = publicationService.listPublishedPublicationsByDate(0);
        assertEquals(0, publications.size());
    }

    //DONE
    @Test
    public void getPublicationsByOwnerReturnsOnlyMostRecentVersionOfPublication() throws ApiGatewayException {
        Publication publication1 = publicationWithIdentifier();
        publication1 = publicationService.createPublication(publication1);
        publicationService.updatePublication(publication1.getIdentifier(), publication1);

        Publication publication2 = publicationWithIdentifier();
        publication2 = publicationService.createPublication(publication2);
        publicationService.updatePublication(publication2.getIdentifier(), publication2);

        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
            OWNER,
            URI.create(PUBLISHER_ID));
        assertEquals(2, publications.size());
    }

    //OBSOLETE
    @Test
    public void listPublishedPublicationsByDateReturnsOnlyMostRecentVersionOfPublication() throws ApiGatewayException {
        Publication publication1 = insertPublishedPublication();
        publicationService.updatePublication(publication1.getIdentifier(), publication1);

        Publication publication2 = insertPublishedPublication();
        publicationService.updatePublication(publication2.getIdentifier(), publication2);

        List<PublicationSummary> publications = publicationService.listPublishedPublicationsByDate(10);
        assertEquals(2, publications.size());
    }

    //DONE
    @Test
    @DisplayName("nonEmpty Table Returns Publications")
    public void nonEmptyTableReturnsPublications() throws ApiGatewayException {
        Publication publication = publicationWithIdentifier();
        publicationService.createPublication(publication);

        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
            OWNER,
            URI.create(PUBLISHER_ID));

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

    //DONE
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

    //Done
    @Test
    public void createPublicationTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        when(failingTable.putItem(any(Item.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService =
            generateFailingService(failingTable, mock(Index.class), mock(Index.class));
        Executable executable = () -> failingService.createPublication(publicationWithIdentifier());
        DynamoDBException exception = assertThrows(DynamoDBException.class, executable);
        assertEquals(ERROR_WRITING_TO_TABLE, exception.getMessage());
    }

    //Done
    @Test
    public void getPublicationTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        when(failingTable.query(any(QuerySpec.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService =
            generateFailingService(failingTable, mock(Index.class), mock(Index.class));
        Executable executable = () -> failingService.getPublication(SortableIdentifier.next());
        DynamoDBException exception = assertThrows(DynamoDBException.class, executable);
        assertEquals(ERROR_READING_FROM_TABLE, exception.getMessage());
    }

    //Done
    @Test
    public void getPublicationsByOwnerTableErrorThrowsException() {
        Index failingIndex = mock(Index.class);
        when(failingIndex.query(any(QuerySpec.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService =
            generateFailingService(mock(Table.class), failingIndex, mock(Index.class));
        Executable executable = () -> failingService.getPublicationsByOwner(OWNER, URI.create(PUBLISHER_ID));
        DynamoDBException exception = assertThrows(DynamoDBException.class, executable);
        assertEquals(ERROR_READING_FROM_TABLE, exception.getMessage());
    }

    //Done
    @Test
    public void updatePublicationTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        when(failingTable.putItem(any(Item.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService =
            generateFailingService(failingTable, mock(Index.class), mock(Index.class));
        Publication publication = publicationWithIdentifier();
        publication.setIdentifier(SortableIdentifier.next());
        Executable executable = () -> failingService.updatePublication(publication.getIdentifier(), publication);
        DynamoDBException exception = assertThrows(DynamoDBException.class, executable);
        assertEquals(ERROR_WRITING_TO_TABLE, exception.getMessage());
    }

    //Obsolete
    @Test
    public void listPublishedPublicationsTableErrorThrowsException() {
        Index failingIndex = mock(Index.class);
        when(failingIndex.query(any(QuerySpec.class))).thenThrow(RuntimeException.class);
        DynamoDBPublicationService failingService =
            generateFailingService(mock(Table.class), mock(Index.class), failingIndex);
        Executable executable = () -> failingService.listPublishedPublicationsByDate(0);
        DynamoDBException exception = assertThrows(DynamoDBException.class, executable);
        assertEquals(ERROR_READING_FROM_TABLE, exception.getMessage());
    }

    //Done
    @Test
    public void publicationToItemThrowsExceptionWhenInvalidJson() throws JsonProcessingException {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(any(Publication.class))).thenThrow(JsonProcessingException.class);
        DynamoDBPublicationService failingService = generateFailingService(failingObjectMapper,
            db.getTable(), db.getByPublisherIndex(), db.getByPublishedDateIndex());
        Executable executable = () -> failingService.publicationToItem(publicationWithIdentifier());
        InputException exception = assertThrows(InputException.class, executable);
        assertEquals(DynamoDBPublicationService.ERROR_MAPPING_PUBLICATION_TO_ITEM, exception.getMessage());
    }

    //Done
    @Test
    public void itemToPublicationThrowsExceptionWhenInvalidJson() {
        Item item = mock(Item.class);
        when(item.toJSON()).thenThrow(new IllegalStateException());
        Executable executable = () -> publicationService.itemToPublication(item);
        DynamoDBException exception = assertThrows(DynamoDBException.class, executable);
        assertEquals(ERROR_MAPPING_ITEM_TO_PUBLICATION, exception.getMessage());
    }

    @Test
    //Done
    public void canPublishPublicationReturnsAccepted() throws Exception {
        Publication publicationToPublish = publicationService.createPublication(publicationWithIdentifier());

        PublishPublicationStatusResponse actual = publicationService
            .publishPublication(publicationToPublish.getIdentifier());

        PublishPublicationStatusResponse expected = new PublishPublicationStatusResponse(
            PUBLISH_IN_PROGRESS, SC_ACCEPTED);
        assertEquals(expected, actual);
    }

    //DONE
    @Test
    public void publishedPublicationHasPublishedDate() throws Exception {
        Publication publicationToPublish = publicationService.createPublication(publicationWithIdentifier());
        publicationService.publishPublication(publicationToPublish.getIdentifier());
        Publication publishedPublication = publicationService.getPublication(publicationToPublish.getIdentifier());
        assertNotNull(publishedPublication.getPublishedDate());
    }

    //DONE
    @Test
    public void publishedPublicationHasStatusPublished() throws Exception {
        Publication publicationToPublish = publicationService.createPublication(publicationWithIdentifier());
        publicationService.publishPublication(publicationToPublish.getIdentifier());
        Publication publishedPublication = publicationService.getPublication(publicationToPublish.getIdentifier());
        assertEquals(PublicationStatus.PUBLISHED, publishedPublication.getStatus());
    }

    //DONE
    @Test
    public void publicationAlreadyPublishedReturnsNoContent() throws Exception {
        Publication publicationToPublish = publicationService.createPublication(publicationWithIdentifier());

        // publish
        publicationService.publishPublication(publicationToPublish.getIdentifier());
        // trying to publish again
        PublishPublicationStatusResponse actual = publicationService
            .publishPublication(publicationToPublish.getIdentifier());

        PublishPublicationStatusResponse expected = new PublishPublicationStatusResponse(
            PUBLISH_COMPLETED, SC_NO_CONTENT);
        assertEquals(expected, actual);
    }

    //Done
    @Test
    public void publishPublicationWithMissingMainTitleReturnsConflict() throws Exception {
        Publication publication = publicationWithIdentifier();
        publication.getEntityDescription().setMainTitle(null);
        Publication publicationToPublish = publicationService.createPublication(publication);
        Executable executable = () -> publicationService.publishPublication(publicationToPublish.getIdentifier());
        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, executable);
        String actualErrorMessage = exception.getMessage();
        assertThat(actualErrorMessage, containsString(ERROR_MESSAGE_TEMPLATE));
        assertEquals(SC_CONFLICT, exception.getStatusCode());
    }

    //Done
    @Test
    public void publishPublicationWithMissingLinkAndFileReturnsConflict() throws Exception {
        Publication publication = publicationWithIdentifier();
        publication.setLink(null);
        publication.setFileSet(new FileSet.Builder().withFiles(List.of()).build());
        Publication publicationToPublish = publicationService.createPublication(publication);
        Executable executable = () -> publicationService.publishPublication(publicationToPublish.getIdentifier());
        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, executable);
        String actualErrorMessage = exception.getMessage();
        assertThat(actualErrorMessage, containsString(ERROR_MESSAGE_TEMPLATE));
        assertEquals(SC_CONFLICT, exception.getStatusCode());
    }

    //Done
    @Test
    public void createPublicationReturnsPublicationWithIdentifierWhenInputIsValid() throws ApiGatewayException {
        Publication publication = publicationWithoutIdentifier();
        assertThat(publication.getIdentifier(), is(nullValue()));
        Publication actual = publicationService.createPublication(publication);
        assertThat(actual.getIdentifier(), is(not(nullValue())));
    }

    //Done
    @Test
    public void deletePublicationCanMarkDraftForDeletion() throws ApiGatewayException {
        Publication publication = publicationWithIdentifier();
        publicationService.createPublication(publication);

        publicationService.markPublicationForDeletion(publication.getIdentifier(), publication.getOwner());

        Publication publicationForDeletion = publicationService.getPublication(publication.getIdentifier());
        assertThat(publicationForDeletion.getStatus(), is(equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    //Done
    @Test
    public void deletePublicationThrowsNotImplementedExceptionWhenDeletingPublishedPublication()
        throws ApiGatewayException {
        Publication publication = insertPublishedPublication();

        assertThrows(NotImplementedException.class,
            () -> publicationService.markPublicationForDeletion(publication.getIdentifier(), publication.getOwner()));
    }

    //Done
    @Test
    public void canDeleteDraftPublicationWithStatusDraftForDeletion() throws ApiGatewayException {
        Publication publication = publicationWithIdentifier();
        publication.setStatus(PublicationStatus.DRAFT_FOR_DELETION);
        Publication createdPublication = publicationService.createPublication(publication);

        publicationService.deleteDraftPublication(createdPublication.getIdentifier());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> publicationService.getPublication(createdPublication.getIdentifier()));
        assertThat(exception, is(instanceOf(NotFoundException.class)));
    }

    //Done
    @Test
    public void canDeleteDraftPublicationWhenPublicationHasMultipleVersions() throws ApiGatewayException {
        Publication publication = publicationWithIdentifier();
        Publication createdPublication = publicationService.createPublication(publication);
        publicationService.markPublicationForDeletion(
            createdPublication.getIdentifier(), createdPublication.getOwner());

        publicationService.deleteDraftPublication(createdPublication.getIdentifier());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> publicationService.getPublication(createdPublication.getIdentifier()));
        assertThat(exception, is(instanceOf(NotFoundException.class)));
    }

    private Publication insertPublishedPublication() throws ApiGatewayException {
        Publication publication = publicationWithIdentifier();
        publication.setStatus(PublicationStatus.PUBLISHED);
        publication.setPublishedDate(Instant.now());
        publication = publicationService.createPublication(publication);
        return publication;
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

    private DynamoDBPublicationService generateFailingService(Table table, Index index, Index otherIndex) {
        return generateFailingService(objectMapper, table, index, otherIndex);
    }

    private DynamoDBPublicationService generateFailingService(ObjectMapper mapper,
                                                              Table table,
                                                              Index index,
                                                              Index otherIndex) {
        return new DynamoDBPublicationService(mapper, table, index, otherIndex);
    }
}