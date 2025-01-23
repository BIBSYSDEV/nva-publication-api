package no.unit.nva.publication.service.impl;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static java.util.Collections.emptyList;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.DRAFT_FOR_DELETION;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomEntityDescription;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.publication.service.impl.UpdateResourceService.ILLEGAL_DELETE_WHEN_NOT_DRAFT;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationNote;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.exception.UnsupportedPublicationStatusTransition;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.model.business.publicationstate.RepublishedResourceEvent;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.Matchers;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceServiceTest extends ResourcesLocalTest {

    private static final String ANOTHER_OWNER = "another@owner.no";
    private static final String SOME_OTHER_USER = "some_other@user.no";
    private static final String UPDATED_TITLE = "UpdatedTitle";
    private static final String SOME_INVALID_FIELD = "someInvalidField";
    private static final String SOME_STRING = "someValue";
    private static final String MAIN_TITLE_FIELD = "mainTitle";
    private static final String ANOTHER_TITLE = "anotherTitle";
    private static final String ENTITY_DESCRIPTION_DOES_NOT_HAVE_FIELD_ERROR = EntityDescription.class.getName()
                                                                               + " does not have a field"
                                                                               + MAIN_TITLE_FIELD;
    private static final Javers JAVERS = JaversBuilder.javers().build();
    private static final int BIG_PAGE = 10;
    private static final URI UNIMPORTANT_AFFILIATION = null;
    private static final URI AFFILIATION_NOT_IMPORTANT = null;
    private static final URI SOME_ORG = randomUri();
    private static final UserInstance SAMPLE_USER = UserInstance.create(randomString(), SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
    private static final String RESOURCE_LACKS_DATA = "Resource does not have required data to be published:";
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String ASSIGNEE = "assignee";
    private static final String FINALIZED_BY = "finalizedBy";
    private static final String OWNER_AFFILIATION = "ownerAffiliation";
    private ResourceService resourceService;

    private TicketService ticketService;
    private MessageService messageService;
    private Instant now;

    @BeforeEach
    public void init() {
        super.init();
        now = Clock.systemDefaultZone().instant();
        resourceService = getResourceServiceBuilder().build();
        ticketService = getTicketService();
        messageService = getMessageService();
    }

    public Optional<ResourceDao> searchForResource(ResourceDao resourceDaoWithStatusDraft) {
        QueryResult queryResult = queryForDraftResource(resourceDaoWithStatusDraft);
        return parseResult(queryResult);
    }

    @Test
    void shouldInstantiateResourceServiceForProvidedTable() {
        var customTable = "CustomTable";
        super.init(customTable);
        var resourceService = getResourceServiceBuilder(client)
                                  .withTableName(customTable)
                                  .build();
        List<String> tableNames = resourceService.getClient().listTables().getTableNames();
        assertThat(tableNames, hasItem(customTable));
    }

    @Test
    void shouldSetImportedEntryCreationModifiedAndPublishedDates() throws NotFoundException {
        var startOfTest = Instant.now();
        var inputPublication = randomPublication().copy()
                                   .withCuratingInstitutions(null)
                                   .withStatus(PUBLISHED)
                                   .build();
        var savedPublicationIdentifier = resourceService.createPublicationFromImportedEntry(inputPublication,
                                                                                            ImportSource.fromSource(
                                                                                                Source.CRISTIN))
                                             .getIdentifier();
        var savedPublication = resourceService.getPublicationByIdentifier(savedPublicationIdentifier);

        assertThat(savedPublication.getCreatedDate(), is(greaterThan(startOfTest)));
        assertThat(savedPublication.getModifiedDate(), is(greaterThan(startOfTest)));
        assertThat(savedPublication.getPublishedDate(), is(greaterThan(startOfTest)));
    }

    @Test
    void createResourceReturnsResourceWithCreatedAndModifiedDateSetByThePlatform() throws ApiGatewayException {

        var input = generatePublication();
        var notExpectedCreatedDate = randomInstant();
        var notExpectedModifiedDate = randomInstant();
        input.setCreatedDate(notExpectedCreatedDate);
        input.setModifiedDate(notExpectedModifiedDate);

        var userInstance = UserInstance.fromPublication(input);
        var savedResource = Resource.fromPublication(input).persistNew(resourceService, userInstance);
        var readResource = resourceService.getPublication(savedResource);
        var expectedResource = input.copy()
                                   .withIdentifier(savedResource.getIdentifier())
                                   .withStatus(DRAFT)
                                   .withCreatedDate(readResource.getCreatedDate())
                                   .withModifiedDate(readResource.getModifiedDate())
                                   .withPublisher(readResource.getPublisher())
                                   .build();
        var diff = JAVERS.compare(expectedResource, savedResource);
        assertThat(diff.prettyPrint(), savedResource, is(equalTo(expectedResource)));
        assertThat(readResource, is(equalTo(expectedResource)));
        assertThat(readResource.getCreatedDate(), is(not(equalTo(notExpectedCreatedDate))));
        assertThat(readResource.getModifiedDate(), is(not(equalTo(notExpectedModifiedDate))));
    }

    @Test
    void createResourceThrowsTransactionFailedExceptionWhenResourceWithSameIdentifierExists()
        throws BadRequestException {
        final Publication sampleResource = randomPublication();
        final Publication collidingResource = sampleResource.copy()
                                                  .withPublisher(anotherPublisher())
                                                  .withResourceOwner(new ResourceOwner(new Username(SOME_OTHER_USER),
                                                                                       null))
                                                  .build();
        ResourceService resourceService = getResourceServiceWithDuplicateIdentifier(sampleResource.getIdentifier());

        createPersistedPublicationWithDoi(resourceService, sampleResource);
        Executable action = () -> createPersistedPublicationWithDoi(resourceService, collidingResource);
        assertThrows(TransactionFailedException.class, action);

        assertThat(sampleResource.getIdentifier(), is(equalTo(collidingResource.getIdentifier())));
        assertThat(sampleResource.getResourceOwner().getOwner(),
                   is(not(equalTo(collidingResource.getResourceOwner().getOwner()))));
        assertThat(sampleResource.getPublisher().getId(), is(not(equalTo(collidingResource.getPublisher().getId()))));
    }

    @Test
    void createResourceSavesResourcesWithSameOwnerAndPublisherButDifferentIdentifier() throws BadRequestException {
        final Publication sampleResource = publicationWithIdentifier();
        final Publication anotherResource = publicationWithIdentifier();

        createPersistedPublicationWithDoi(resourceService, sampleResource);
        assertDoesNotThrow(() -> createPersistedPublicationWithDoi(resourceService, anotherResource));
    }

    @Test
    void getResourceByIdentifierReturnsNotFoundWhenResourceDoesNotExist() {
        SortableIdentifier nonExistingIdentifier = SortableIdentifier.next();
        Executable action = () -> resourceService.getPublicationByIdentifier(nonExistingIdentifier);
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void getResourceByIdentifierReturnsResourceWhenResourceExists() throws ApiGatewayException {
        Publication sampleResource = createPersistedPublicationWithDoi();
        UserInstance userInstance = UserInstance.fromPublication(sampleResource);
        Publication savedResource = resourceService.getPublicationByIdentifier(sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }

    @Test
    void whenPublicationOwnerIsUpdatedTheResourceEntryMaintainsTheRestResourceMetadata() throws ApiGatewayException {
        var originalResource = createPersistedPublicationWithDoi();

        var oldOwner = UserInstance.fromPublication(originalResource);
        var newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(originalResource);

        var newResource = resourceService.getPublicationByIdentifier(originalResource.getIdentifier());

        var expectedResource = expectedUpdatedResource(originalResource, newResource, newOwner);
        var diff = JAVERS.compare(expectedResource, newResource);
        assertThat(diff.prettyPrint(), newResource, is(equalTo(expectedResource)));
    }

    @Test
    void whenPublicationOwnerIsUpdatedThenBothOrganizationAndUserAreUpdated() throws ApiGatewayException {
        Publication originalResource = createPersistedPublicationWithDoi();
        UserInstance oldOwner = UserInstance.fromPublication(originalResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);

        Publication newResource = resourceService.getPublicationByIdentifier(originalResource.getIdentifier());

        assertThat(newResource.getResourceOwner().getOwner().getValue(), is(equalTo(newOwner.getUsername())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getCustomerId())));
    }

    @Test
    void whenPublicationOwnerIsUpdatedTheModifiedDateIsUpdated() throws ApiGatewayException {
        Publication sampleResource = createPersistedPublicationWithDoi();
        UserInstance oldOwner = UserInstance.fromPublication(sampleResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Publication newResource = resourceService.getPublicationByIdentifier(sampleResource.getIdentifier());

        assertThat(newResource.getModifiedDate(), is(greaterThan(newResource.getCreatedDate())));
    }

    @Test
    void resourceIsUpdatedWhenResourceUpdateIsReceived() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        Publication actualOriginalResource = resourceService.getPublication(resource);
        assertThat(actualOriginalResource, is(equalTo(resource)));

        Publication resourceUpdate = updateResourceTitle(resource);
        resourceService.updatePublication(resourceUpdate);
        Publication actualUpdatedResource = resourceService.getPublication(resource);

        assertThat(actualUpdatedResource, is(equalTo(resourceUpdate)));
        assertThat(actualUpdatedResource, is(not(equalTo(actualOriginalResource))));
    }

    @Test
    void resourceUpdateFailsWhenUpdateChangesTheOwnerPartOfThePrimaryKey() throws BadRequestException {
        Publication resource = createPersistedPublicationWithDoi();
        Publication resourceUpdate = updateResourceTitle(resource);
        resourceUpdate.setResourceOwner(new ResourceOwner(new Username(ANOTHER_OWNER), UNIMPORTANT_AFFILIATION));
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    void resourceUpdateFailsWhenUpdateChangesTheOrganizationPartOfThePrimaryKey() throws BadRequestException {
        Publication resource = createPersistedPublicationWithDoi();
        Publication resourceUpdate = updateResourceTitle(resource);

        resourceUpdate.setPublisher(newOrganization());
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    void resourceUpdateFailsWhenUpdateChangesTheIdentifierPartOfThePrimaryKey() throws BadRequestException {
        Publication resource = createPersistedPublicationWithDoi();
        Publication resourceUpdate = updateResourceTitle(resource);

        resourceUpdate.setIdentifier(SortableIdentifier.next());
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    void createResourceThrowsTransactionFailedExceptionWithInternalCauseWhenCreatingResourceFails() {
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        String expectedMessage = "expectedMessage";
        RuntimeException expectedCause = new RuntimeException(expectedMessage);
        when(client.transactWriteItems(any(TransactWriteItemsRequest.class))).thenThrow(expectedCause);

        ResourceService failingService = getResourceServiceBuilder(client).build();

        Publication resource = publicationWithIdentifier();
        Executable action = () -> createPersistedPublicationWithDoi(failingService, resource);
        TransactionFailedException actualException = assertThrows(TransactionFailedException.class, action);
        Throwable actualCause = actualException.getCause();
        assertThat(actualCause.getMessage(), is(equalTo(expectedMessage)));
    }

    @Test
    void insertPreexistingPublicationIdentifierStoresPublicationInDatabaseWithoutChangingIdentifier() {
        Publication publication = publicationWithIdentifier();
        Publication savedPublication = resourceService.insertPreexistingPublication(publication);
        assertThat(savedPublication.getIdentifier(), is(equalTo(publication.getIdentifier())));
    }

    @Test
    void getResourcePropagatesExceptionWithWhenGettingResourceFailsForUnknownReason() {
        var client = mock(AmazonDynamoDB.class);
        var expectedMessage = new RuntimeException("expectedMessage");
        when(client.getItem(any(GetItemRequest.class))).thenThrow(expectedMessage);
        var resource = publicationWithIdentifier();

        var failingResourceService = getResourceServiceBuilder(client).build();

        Executable action = () -> failingResourceService.getPublication(resource);
        var exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), is(equalTo(expectedMessage.getMessage())));
    }

    @ParameterizedTest(name = "Should return publication by owner when status is {0}")
    @EnumSource(value = PublicationStatus.class, mode = Mode.EXCLUDE, names = {"DRAFT_FOR_DELETION"})
    void shouldReturnPublicationsForOwnerWithStatus(PublicationStatus status) {
        var userInstance = UserInstance.create(randomString(), randomUri());
        var publication = createPublicationWithStatus(userInstance, status);
        var actualResources = resourceService.getPublicationsByOwner(userInstance);
        var resourceSet = new HashSet<>(actualResources);
        assertThat(resourceSet, containsInAnyOrder(publication));
    }

    @Test
    void getResourcesByOwnerReturnsAllResourcesOwnedByUser() {
        UserInstance userInstance = UserInstance.create(randomString(), randomUri());
        Set<Publication> userResources = createSamplePublicationsOfSingleOwner(userInstance);

        List<Publication> actualResources = resourceService.getPublicationsByOwner(userInstance);
        HashSet<Publication> actualResourcesSet = new HashSet<>(actualResources);

        assertThat(actualResourcesSet, containsInAnyOrder(userResources.toArray(Publication[]::new)));
    }

    @Test
    void getResourcesByCristinIdentifierReturnsAllResourcesWithCristinIdentifier() {
        String cristinIdentifier = randomString();
        createSamplePublicationsOfSingleCristinIdentifier(cristinIdentifier);
        List<Publication> actualPublication = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);
        HashSet<Publication> actualResourcesSet = new HashSet<>(actualPublication);
        assertTrue(actualPublication.containsAll(actualResourcesSet));
    }

    @Test
    void itIsNotPossibleToPersistMultipleTrustedCristinIdentifiersWhenUpdatedPublication()
        throws BadRequestException, NotFoundException {

        Publication resource = createPersistedPublicationWithDoi();
        Publication actualOriginalResource = resourceService.getPublication(resource);
        assertThat(actualOriginalResource, is(equalTo(resource)));

        Set<AdditionalIdentifierBase> multipleTrustedCristinIdentifiers = Set.of(
            new AdditionalIdentifier("Cristin", randomString()),
            new AdditionalIdentifier("Cristin", randomString()));
        var updatedResource = resource.copy().withAdditionalIdentifiers(multipleTrustedCristinIdentifiers).build();
        assertThrows(IllegalArgumentException.class, () -> resourceService.updatePublication(updatedResource));
    }

    @Test
    @Disabled //TODO: Decide how to manage multiple cristin identifiers that exist in the data
    void combinationOfTrustedAndUntrustedCristinIdentifiersIsNotAllowed()
        throws BadRequestException, NotFoundException {

        Publication resource = createPersistedPublicationWithDoi();
        Publication actualOriginalResource = resourceService.getPublication(resource);
        assertThat(actualOriginalResource, is(equalTo(resource)));

        Set<AdditionalIdentifierBase> cristinIdentifiers = Set.of(
            new AdditionalIdentifier("Cristin", randomString()),
            new CristinIdentifier(SourceName.fromBrage("uit"), randomString()));
        var updatedResource = resource.copy().withAdditionalIdentifiers(cristinIdentifiers).build();

        assertThrows(IllegalArgumentException.class, () -> resourceService.updatePublication(updatedResource));
    }

    @Test
    void getResourcesByOwnerReturnsEmptyListWhenUseHasNoPublications() {

        List<Publication> actualResources = resourceService.getPublicationsByOwner(SAMPLE_USER);
        HashSet<Publication> actualResourcesSet = new HashSet<>(actualResources);

        assertThat(actualResourcesSet, is(equalTo(Collections.emptySet())));
    }

    @Test
    void getResourcesByOwnerPropagatesExceptionWhenExceptionIsThrown() {
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        String expectedMessage = "expectedMessage";
        RuntimeException expectedException = new RuntimeException(expectedMessage);
        when(client.query(any(QueryRequest.class))).thenThrow(expectedException);

        var failingResourceService = getResourceServiceBuilder(client).build();

        RuntimeException exception = assertThrows(RuntimeException.class,
                                                  () -> failingResourceService.getPublicationsByOwner(SAMPLE_USER));

        assertThat(exception.getMessage(), is(equalTo(expectedMessage)));
    }

    @Test
    void getResourcesByOwnerPropagatesJsonProcessingExceptionWhenExceptionIsThrown() {
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        Item invalidItem = new Item().withString(SOME_INVALID_FIELD, SOME_STRING);
        QueryResult responseWithInvalidItem = new QueryResult().withItems(
            List.of(ItemUtils.toAttributeValues(invalidItem)));
        when(mockClient.query(any(QueryRequest.class))).thenReturn(responseWithInvalidItem);

        ResourceService failingResourceService = getResourceServiceBuilder(mockClient).build();
        Class<JsonProcessingException> expectedExceptionClass = JsonProcessingException.class;

        assertThatJsonProcessingErrorIsPropagatedUp(expectedExceptionClass,
                                                    () -> failingResourceService.getPublicationsByOwner(SAMPLE_USER));
    }

    @Test
    void getResourcePropagatesJsonProcessingExceptionWhenExceptionIsThrown() {

        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        Item invalidItem = new Item().withString(SOME_INVALID_FIELD, SOME_STRING);
        var responseWithInvalidItem = new QueryResult().withItems(List.of(ItemUtils.toAttributeValues(invalidItem)));
        when(mockClient.query(any())).thenReturn(responseWithInvalidItem);

        ResourceService failingResourceService = getResourceServiceBuilder(mockClient).build();
        Class<JsonProcessingException> expectedExceptionClass = JsonProcessingException.class;

        SortableIdentifier someIdentifier = SortableIdentifier.next();
        Executable action = () -> failingResourceService.getPublicationByIdentifier(someIdentifier);

        assertThatJsonProcessingErrorIsPropagatedUp(expectedExceptionClass, action);
    }

    @Test
    void shouldPublishResourceWhenClientRequestsToPublish() throws ApiGatewayException {
        var publication = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(publication);
        Resource.fromPublication(publication).publish(resourceService, userInstance);
        var actualPublication = resourceService.getPublication(publication);
        var expectedPublication = publication.copy()
                                   .withStatus(PUBLISHED)
                                   .withModifiedDate(actualPublication.getModifiedDate())
                                   .withPublishedDate(actualPublication.getPublishedDate())
                                   .build();

        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void publishPublicationReturnsResponseThatRequestWasAcceptedWhenResourceIsNotPublished()
        throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();

        UserInstance userInstance = UserInstance.fromPublication(resource);
        PublishPublicationStatusResponse response = resourceService.publishPublication(userInstance,
                                                                                       resource.getIdentifier());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    void publishPublicationReturnsPublicationResponseThatNoActionWasTakenWhenResourceIsAlreadyPublished()
        throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();

        UserInstance userInstance = UserInstance.fromPublication(resource);
        resourceService.publishPublication(userInstance, resource.getIdentifier());
        PublishPublicationStatusResponse response = resourceService.publishPublication(userInstance,
                                                                                       resource.getIdentifier());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NO_CONTENT)));
    }

    @Test
    void byTypeCustomerStatusIndexIsUpdatedWhenResourceIsUpdated() throws ApiGatewayException {
        Publication resourceWithStatusDraft = createPersistedPublicationWithDoi();
        ResourceDao resourceDaoWithStatusDraft = new ResourceDao(Resource.fromPublication(resourceWithStatusDraft));

        assertThatResourceCanBeFoundInDraftResources(resourceDaoWithStatusDraft);

        resourceService.publishPublication(UserInstance.fromPublication(resourceWithStatusDraft),
                                           resourceWithStatusDraft.getIdentifier());

        verifyThatTheResourceWasMovedFromTheDrafts(resourceDaoWithStatusDraft);

        verifyThatTheResourceIsInThePublishedResources(resourceWithStatusDraft);
    }

    @Test
    void publishPublicationSetsPublishedDateToBeCurrentDate() throws ApiGatewayException {
        Publication updatedResource = createPublishedResource();
        assertThat(updatedResource.getPublishedDate(), is(greaterThan(now)));
    }

    @Test
    void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoTitle() throws BadRequestException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.getEntityDescription().setMainTitle(null);
        Publication savedResource = createPersistedPublicationWithoutDoi(sampleResource);

        Executable action = () -> resourceService.publishPublication(UserInstance.fromPublication(sampleResource),
                                                                     savedResource.getIdentifier());

        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, containsString(RESOURCE_LACKS_DATA));
    }

    @Test
    void publishResourcePublishesResourceWhenDoiIsPresentButNoFiles() throws ApiGatewayException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.getEntityDescription().getReference().setDoi(randomDoi());
        sampleResource.setAssociatedArtifacts(createEmptyArtifactList());
        Publication savedResource = createPersistedPublicationWithoutDoi();
        Publication updatedResource = publishResource(savedResource);
        assertThat(updatedResource.getStatus().toString(), is(equalTo(PUBLISHED.toString())));
    }

    @Test
    void publishResourcePublishesResourceWhenResourceHasFilesButNoDoi() throws ApiGatewayException {

        Publication sampleResource = createPersistedPublicationWithoutDoi();
        sampleResource.setLink(null);
        sampleResource.getEntityDescription().getReference().setDoi(null);

        Publication updatedResource = publishResource(sampleResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PUBLISHED)));
    }

    @Test
    void shouldKeepTheResourceInSyncWithTheAssociatedDoiRequestWhenResourceIsPublished() throws ApiGatewayException {
        var publication = createPersistedPublicationWithoutDoi();

        var doiRequest = DoiRequest.fromPublication(publication);
        doiRequest.persistNewTicket(ticketService);
        assertThat(doiRequest.getResourceStatus(), is(equalTo(PublicationStatus.DRAFT)));

        var publishedResource = publishResource(publication);
        assertThat(publishedResource.getStatus(), is(equalTo(PUBLISHED)));

        var actualDoiRequest = (DoiRequest) ticketService.fetchTicket(doiRequest);
        assertThat(actualDoiRequest.getResourceStatus(), is(equalTo(PUBLISHED)));
    }

    @Test
    void markPublicationForDeletionThrowsExceptionWhenDeletingPublishedPublication() throws ApiGatewayException {
        Publication resource = createPublishedResource();
        Executable action = () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                                                                             resource.getIdentifier());
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE));
        assertThat(exception.getMessage(), containsString(resource.getIdentifier().toString()));
    }

    @Test
    void markPublicationForDeletionThrowsExceptionWhenDeletingSomeoneElsePublication() throws ApiGatewayException {
        Publication resource = createPublishedResource();
        var userInstance = UserInstance.create(randomString(), randomUri());
        Executable action = () -> resourceService.markPublicationForDeletion(userInstance,
                                                                             resource.getIdentifier());
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(RESOURCE_NOT_FOUND_MESSAGE));
        assertThat(exception.getMessage(), containsString(resource.getIdentifier().toString()));
    }

    @Test
    void markPublicationForDeletionLogsConditionExceptionWhenUpdateConditionFails() throws ApiGatewayException {
        TestAppender testAppender = LogUtils.getTestingAppender(ResourceService.class);
        Publication resource = createPublishedResource();
        Executable action = () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                                                                             resource.getIdentifier());
        assertThrows(BadRequestException.class, action);
        assertThat(testAppender.getMessages(), containsString(ILLEGAL_DELETE_WHEN_NOT_DRAFT));
    }

    @Test
    void createResourceReturnsNewIdentifierWhenResourceIsCreated() throws BadRequestException {
        Publication sampleResource = randomPublication();
        Publication savedResource = createPersistedPublicationWithDoi(resourceService, sampleResource);
        assertThat(savedResource.getIdentifier(), is(not(equalTo(sampleResource.getIdentifier()))));
    }

    @Test
    void deletePublicationCanMarkDraftForDeletion() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();

        Publication resourceUpdate = resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                                                                                resource.getIdentifier());
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        Publication resourceForDeletion = resourceService.getPublication(resource);
        assertThat(resourceForDeletion.getStatus(),
                   Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    void markPublicationForDeletionReturnsUpdatedResourceWithStatusDraftForDeletion() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(resource);
        var resourceUpdate = resourceService.markPublicationForDeletion(userInstance, resource.getIdentifier());
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    void markPublicationForDeletionResourceThrowsErrorWhenDeletingPublicationThatIsMarkedForDeletion()
        throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource), resource.getIdentifier());
        Publication actualResource = resourceService.getPublication(resource);
        assertThat(actualResource.getStatus(), is(equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        Executable action = () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                                                                             resource.getIdentifier());
        assertThrows(BadRequestException.class, action);
    }

    @Test
    void updateResourceUpdatesAllFieldsInDoiRequest() throws ApiGatewayException {
        var initialPublication = createPersistedPublicationWithoutDoi();
        var initialDoiRequest = createDoiRequest(initialPublication);
        var publicationUpdate = updateAllPublicationFieldsExpectIdentifierStatusAndOwnerInfo(initialPublication);
        resourceService.updatePublication(publicationUpdate);

        var updatedDoiRequest = (DoiRequest) ticketService.fetchTicket(initialDoiRequest);

        var expectedDoiRequest = expectedDoiRequestAfterPublicationUpdate(initialPublication, initialDoiRequest,
                                                                          publicationUpdate, updatedDoiRequest);

        assertThat(updatedDoiRequest, doesNotHaveEmptyValuesIgnoringFields(Set.of(OWNER_AFFILIATION, ASSIGNEE,
                                                                                  FINALIZED_BY,
                                                                                  FINALIZED_DATE)));
        assertThat(expectedDoiRequest, doesNotHaveEmptyValuesIgnoringFields(Set.of(OWNER_AFFILIATION, ASSIGNEE,
                                                                                   FINALIZED_BY,
                                                                                   FINALIZED_DATE)));
        Diff diff = JAVERS.compare(updatedDoiRequest, expectedDoiRequest);
        assertThat(diff.prettyPrint(), updatedDoiRequest, is(equalTo(expectedDoiRequest)));
    }

    @Test
    void updateResourceDoesNotCreateDoiRequestWhenItDoesNotPreexist() throws BadRequestException {
        Publication resource = createPersistedPublicationWithoutDoi();
        resource.getEntityDescription().setMainTitle(ANOTHER_TITLE);
        resourceService.updatePublication(resource);

        var expectedNonExistingTicket = ticketService.fetchTicketByResourceIdentifier(resource.getPublisher().getId(),
                                                                                      resource.getIdentifier(),
                                                                                      DoiRequest.class);

        assertThat(expectedNonExistingTicket, is(emptyOptional()));
    }

    @Test
    void deleteDraftPublicationDeletesDraftResourceWithoutDoi() throws ApiGatewayException {
        var publication = createPersistedPublicationWithoutDoi();
        assertThatIdentifierEntryHasBeenCreated();

        Executable fetchResourceAction = () -> resourceService.getPublication(publication);
        assertDoesNotThrow(fetchResourceAction);

        var userInstance = UserInstance.fromPublication(publication);
        resourceService.deleteDraftPublication(userInstance, publication.getIdentifier());
        assertThrows(NotFoundException.class, fetchResourceAction);

        assertThatAllEntriesHaveBeenDeleted();
    }

    @Test
    void deleteDraftPublicationThrowsExceptionWhenResourceHasDoi() throws BadRequestException {
        Publication publication = createPersistedPublicationWithDoi();
        assertThatIdentifierEntryHasBeenCreated();
        Executable fetchResourceAction = () -> resourceService.getPublication(publication);
        assertDoesNotThrow(fetchResourceAction);

        UserInstance userInstance = UserInstance.fromPublication(publication);
        Executable deleteAction = () -> resourceService.deleteDraftPublication(userInstance,
                                                                               publication.getIdentifier());
        assertThrows(TransactionFailedException.class, deleteAction);

        assertThatTheEntriesHaveNotBeenDeleted();
    }

    @Test
    void deleteDraftPublicationThrowsExceptionWhenResourceIsPublished() throws ApiGatewayException {
        Publication publication = createPersistedPublicationWithoutDoi();
        UserInstance userInstance = UserInstance.fromPublication(publication);
        resourceService.publishPublication(userInstance, publication.getIdentifier());
        assertThatIdentifierEntryHasBeenCreated();

        Executable fetchResourceAction = () -> resourceService.getPublication(publication);
        assertDoesNotThrow(fetchResourceAction);

        Executable deleteAction = () -> resourceService.deleteDraftPublication(userInstance,
                                                                               publication.getIdentifier());
        assertThrows(TransactionFailedException.class, deleteAction);

        assertThatTheEntriesHaveNotBeenDeleted();
    }

    @Test
    void deleteDraftPublicationDeletesDoiRequestWhenPublicationHasDoiRequest() throws ApiGatewayException {
        Publication publication = createPersistedPublicationWithoutDoi();
        createDoiRequest(publication);

        UserInstance userInstance = UserInstance.fromPublication(publication);
        resourceService.deleteDraftPublication(userInstance, publication.getIdentifier());

        assertThatAllEntriesHaveBeenDeleted();
    }

    @Test
    void getResourceByIdentifierReturnsExistingResource() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();

        Publication retrievedResource = resourceService.getPublicationByIdentifier(resource.getIdentifier());
        assertThat(retrievedResource, is(equalTo(resource)));
    }

    @Test
    void getResourceByIdentifierThrowsNotFoundExceptionWhenResourceDoesNotExist() {
        SortableIdentifier someIdentifier = SortableIdentifier.next();
        Executable action = () -> resourceService.getPublicationByIdentifier(someIdentifier);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(someIdentifier.toString()));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldScanEntriesInDatabaseAfterSpecifiedMarker(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

        var userInstance = UserInstance.fromPublication(publication);

        var sampleMessage = messageService.createMessage(ticket, userInstance, randomString());

        var firstListingResult = fetchFirstDataEntry();
        var identifierInFirstScan = extractIdentifierFromFirstScanResult(firstListingResult);

        var secondListingResult = fetchRestOfDatabaseEntries(firstListingResult);
        var identifiersFromSecondScan = secondListingResult.getDatabaseEntries()
                                            .stream()
                                            .map(Entity::getIdentifier)
                                            .toList();

        var expectedIdentifiers = new ArrayList<>(
            List.of(publication.getIdentifier(), ticket.getIdentifier(), sampleMessage.getIdentifier()));
        expectedIdentifiers.remove(identifierInFirstScan);
        assertThat(identifiersFromSecondScan,
                   containsInAnyOrder(expectedIdentifiers.toArray(SortableIdentifier[]::new)));
    }

    @Test
    void shouldLogUserInformationQueryObjectAndResourceIdentifierWhenFailingToPublishResource()
        throws BadRequestException {

        var samplePublication = createUnpublishablePublication();
        var userInstance = UserInstance.fromPublication(samplePublication);
        var exception = assertThrows(InvalidPublicationException.class,
                                     () -> resourceService.publishPublication(userInstance,
                                                                              samplePublication.getIdentifier()));
        assertThat(exception.getMessage(), containsString(RESOURCE_LACKS_DATA));
    }

    @Test
    void shouldMaintainAssociatedArtifactsThatOtherThanFile() throws ApiGatewayException {
        var publication = draftPublicationWithoutDoiAndAssociatedLink();
        var persistedDraft = Resource.fromPublication(publication)
                                 .persistNew(resourceService, UserInstance.fromPublication(publication));

        resourceService.publishPublication(UserInstance.fromPublication(persistedDraft),
                                           persistedDraft.getIdentifier());
        var persistedPublished = resourceService.getPublication(persistedDraft);
        assertThat(persistedPublished.getStatus(), is(equalTo(PUBLISHED)));
        assertThat(persistedPublished.getAssociatedArtifacts(), everyItem(is(instanceOf(AssociatedLink.class))));
    }

    @Test
    void shouldBePossibleToSetPublicationStatusToDeletedForPublishedPublication() throws ApiGatewayException {
        var publishedResource = createPublishedResource();
        var publicationIdentifier = publishedResource.getIdentifier();
        var expectedUpdateStatus = UpdateResourceService.deletionStatusChangeInProgress();
        var actualUpdateStatus = resourceService.updatePublishedStatusToDeleted(publicationIdentifier);
        assertThat(actualUpdateStatus, is(equalTo(expectedUpdateStatus)));
        var actualPublicationInDatabaseAfterStatusUpdate =
            resourceService.getPublicationByIdentifier(publicationIdentifier);
        assertThat(actualPublicationInDatabaseAfterStatusUpdate.getStatus(), is(equalTo(PublicationStatus.DELETED)));
        assertThat(actualPublicationInDatabaseAfterStatusUpdate.getPublishedDate(), is(equalTo(null)));
    }

    @Test
    void updatePublishedStatusToDeletedShouldReturnResourceAlreadyDeletedMessage() throws ApiGatewayException {
        var publishedResource = createPublishedResource();
        var publicationIdentifier = publishedResource.getIdentifier();
        var expectedUpdateStatus = UpdateResourceService.deletionStatusIsCompleted();
        resourceService.updatePublishedStatusToDeleted(publicationIdentifier);
        var actualUpdateStatus = resourceService.updatePublishedStatusToDeleted(publicationIdentifier);
        assertThat(expectedUpdateStatus, is(equalTo(actualUpdateStatus)));
    }

    @Test
    void shouldSetCuratingInstitutionsWhenUpdatingPublication() throws ApiGatewayException {
        var publishedResource = createPublishedResource();
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
        when(uriRetriever.getRawContent(eq(orgId), any())).thenReturn(
            Optional.of(IoUtils.stringFromResources(Path.of("cristin-orgs/20754.6.0.0.json"))));
        var affiliation = (new Organization.Builder())
                              .withId(orgId)
                              .build();
        publishedResource.getEntityDescription().setContributors(List.of(randomContributor(List.of(affiliation))));

        var updatedResource = resourceService.updatePublication(publishedResource);

        assertThat(updatedResource.getCuratingInstitutions().stream().findFirst().orElseThrow().id(),
                   is(equalTo(topLevelId)));
    }

    @Test
    void shouldNotUpdatePublicationCuratingInstitutionsWhenContributorsAreUnchanged() throws ApiGatewayException {
        var resource = createPersistedPublicationWithoutDoi();
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

        var affiliation = (new Organization.Builder())
                              .withId(orgId)
                              .build();

        resource.getEntityDescription().setContributors(List.of(randomContributor(List.of(affiliation))));
        resource.setCuratingInstitutions(Set.of(new CuratingInstitution(topLevelId, Set.of(randomUri()))));
        var publishedResource = publishResource(createPersistedPublicationWithoutDoi(resource));

        var updatedResource = resourceService.updatePublication(publishedResource);

        verify(uriRetriever, never()).getRawContent(eq(orgId), any());
        assertThat(updatedResource.getCuratingInstitutions().stream().findFirst().orElseThrow().id(),
                   is(equalTo(topLevelId)));
    }

    @Test
    void shouldSetCuratingInstitutionsWhenUpdatingImportCandidate() throws ApiGatewayException {
        var importCandidate = randomImportCandidate();
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
        when(uriRetriever.getRawContent(eq(orgId), any())).thenReturn(
            Optional.of(IoUtils.stringFromResources(Path.of("cristin-orgs/20754.6.0.0.json"))));

        var persistedImportCandidate = resourceService.persistImportCandidate(importCandidate);

        var affiliation = (new Organization.Builder())
                              .withId(orgId)
                              .build();
        persistedImportCandidate.getEntityDescription()
            .setContributors(List.of(randomContributor(List.of(affiliation))));

        var updatedImportCandidate = resourceService.updateImportCandidate(persistedImportCandidate);

        assertThat(updatedImportCandidate.getCuratingInstitutions().stream().findFirst().orElseThrow().id(),
                   is(equalTo(topLevelId)));
    }

    @Test
    void shouldNotSetCuratingInstitutionsWhenUpdatingImportCandidateWhenContributorsAreUnchanged()
        throws ApiGatewayException {
        var importCandidate = randomImportCandidate();
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

        var affiliation = (new Organization.Builder())
                              .withId(orgId)
                              .build();

        importCandidate.getEntityDescription().setContributors(List.of(randomContributor(List.of(affiliation))));
        importCandidate.setCuratingInstitutions(Set.of(new CuratingInstitution(topLevelId, Set.of(randomUri()))));

        var persistedImportCandidate = resourceService.persistImportCandidate(importCandidate);

        var updatedImportCandidate = resourceService.updateImportCandidate(persistedImportCandidate);

        verify(uriRetriever, never()).getRawContent(eq(orgId), any());
        assertThat(updatedImportCandidate.getCuratingInstitutions().stream().findFirst().orElseThrow().id(),
                   is(equalTo(topLevelId)));
    }

    @Test
    void shouldSetCuratingInstitutionsWhenUpdatingNewPublicationWithoutEntityDescription() throws ApiGatewayException {
        var template = randomPublication().copy();
        var entityDescription = template.build().getEntityDescription();
        var publication = template.withDoi(null).withEntityDescription(null).build();
        publication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                       UserInstance.fromPublication(publication));
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        final var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
        when(uriRetriever.getRawContent(eq(orgId), any())).thenReturn(
            Optional.of(IoUtils.stringFromResources(Path.of("cristin-orgs/20754.6.0.0.json"))));

        var affiliation = (new Organization.Builder())
                              .withId(orgId)
                              .build();
        entityDescription.setContributors(List.of(randomContributor(List.of(affiliation))));
        publication.setEntityDescription(entityDescription);

        var updatedResource = resourceService.updatePublication(publication);

        assertThat(updatedResource.getCuratingInstitutions().stream().findFirst().orElseThrow().id(),
                   is(equalTo(topLevelId)));
    }

    @Test
    void shouldCreateResourceFromImportCandidate() throws NotFoundException {
        var importCandidate = randomImportCandidate();
        var persistedImportCandidate = resourceService.persistImportCandidate(importCandidate);
        var fetchedImportCandidate = resourceService.getImportCandidateByIdentifier(
            persistedImportCandidate.getIdentifier());
        assertThat(persistedImportCandidate.getImportStatus(), is(equalTo(fetchedImportCandidate.getImportStatus())));
        assertThat(persistedImportCandidate, is(equalTo(fetchedImportCandidate)));
    }

    @Test
    void shouldCreatePublicationWithStatusPublishedWhenUsingAutoImport() throws NotFoundException {
        var publication = randomImportCandidate();
        var persistedPublication = resourceService.autoImportPublicationFromScopus(publication);
        var fetchedPublication = resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        assertThat(persistedPublication, is(equalTo(fetchedPublication)));
        assertThat(fetchedPublication.getStatus(), is(equalTo(PUBLISHED)));
    }

    @Test
    void shouldUpdateImportStatus() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        var expectedStatus = ImportStatusFactory.createImported(randomPerson(), randomUri());
        resourceService.updateImportStatus(importCandidate.getIdentifier(), expectedStatus);
        var fetchedPublication = resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
        assertThat(fetchedPublication.getImportStatus(), is(equalTo(expectedStatus)));
    }

    @Test
    void shouldProvidePublicationNotesWhenTheyAreSet() throws BadRequestException, NotFoundException {
        var publication = randomPublication();
        publication.setPublicationNotes(List.of(new PublicationNote(randomString())));
        var result = Resource.fromPublication(publication).persistNew(resourceService,
                                                                      UserInstance.fromPublication(publication));
        var storedPublication = resourceService.getPublication(result);
        assertThat(storedPublication.getPublicationNotes(), is(equalTo(publication.getPublicationNotes())));
    }

    @Test
    void shouldLogIdentifiersOfRecordsWhenBatchScanWriteFails() {
        var failingClient = new FailingDynamoClient(this.client);
        resourceService = getResourceServiceBuilder(failingClient).build();

        var userInstance = UserInstance.create(randomString(), randomUri());
        var userResources = createSamplePublicationsOfSingleOwner(userInstance);
        // correctness of this test rely on number of publications generated above does not exceed
        // ResourceService.MAX_SIZE_OF_BATCH_REQUEST
        var resources = userResources.stream()
                            .map(Resource::fromPublication)
                            .map(Entity.class::cast)
                            .toList();

        var testAppender = LogUtils.getTestingAppenderForRootLogger();

        resourceService.refreshResources(resources);

        assertThatFailedBatchScanLogsProperly(testAppender, userResources);
    }

    @Test
    void shouldReturnResourceWithContributorsWhenResourceHasManyContributions()
        throws BadRequestException, NotFoundException {
        var publication = createPersistedPublicationWithManyContributions(4000);

        var fetchedPublication = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var fetchedContributors = fetchedPublication.getEntityDescription().getContributors();
        assertThat(fetchedContributors.size(), is(equalTo(4000)));
    }

    @Test
    void shouldReturnResourceWithContributorsWhenResourceHasManyContributionsWithoutAffiliations()
        throws BadRequestException, NotFoundException {
        var publication = createPersistedPublicationWithManyContributionsWithoutAffiliations(10000);

        var fetchedPublication = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var fetchedContributors = fetchedPublication.getEntityDescription().getContributors();
        assertThat(fetchedContributors.size(), is(equalTo(10000)));
    }

    @Test
    void shouldThrowBadRequestWhenUnpublishingNotPublishedPublication() throws ApiGatewayException {
        var publication = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(publication);
        assertThrows(BadRequestException.class, () -> resourceService.unpublishPublication(publication, userInstance));
    }

    @Test
    void shouldSetPublicationStatusToUnpublishedWhenUnpublishingPublication() throws ApiGatewayException {
        var publication = createPublishedResource();
        var userInstance = UserInstance.fromPublication(publication);
        resourceService.unpublishPublication(publication, userInstance);
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(UNPUBLISHED)));
    }

    @Test
    void shouldCreateAPendingUnpublishingRequestTicketWhenUnpublishingPublication() throws ApiGatewayException {
        var publication = createPublishedResource();
        var userInstance = UserInstance.fromPublication(publication);
        resourceService.unpublishPublication(publication, userInstance);
        var tickets = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication)).toList();
        assertThat(tickets, hasSize(1));
        assertThat(tickets, hasItem(allOf(instanceOf(UnpublishRequest.class),
                                          hasProperty("status", is(equalTo(TicketStatus.PENDING))))));
    }

    @Test
    void shouldSetAllPendingTicketsToNotApplicableWhenUnpublishingPublication() throws ApiGatewayException {
        var publication = createPublishedResource();
        var username = UserInstance.fromPublication(publication).getUsername();
        GeneralSupportRequest.fromPublication(publication).withOwner(username).persistNewTicket(ticketService);
        DoiRequest.fromPublication(publication).withOwner(username).persistNewTicket(ticketService);
        var closedGeneralSupportTicket =
            GeneralSupportRequest.fromPublication(publication).withOwner(username).persistNewTicket(ticketService)
                .close(new Username(randomString()));
        ticketService.updateTicket(closedGeneralSupportTicket);
        var publishingRequestTicket = PublishingRequestCase.fromPublication(publication).withOwner(username);
        publishingRequestTicket.setStatus(TicketStatus.COMPLETED);
        publishingRequestTicket.persistNewTicket(ticketService);
        var userInstance = UserInstance.fromPublication(publication);
        resourceService.unpublishPublication(publication, userInstance);
        var tickets = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication)).toList();
        assertThat(tickets, hasSize(5));
        assertThat(tickets, hasItem(allOf(instanceOf(GeneralSupportRequest.class),
                                          hasProperty("status", is(equalTo(TicketStatus.CLOSED))))));
        assertThat(tickets, hasItem(allOf(instanceOf(GeneralSupportRequest.class),
                                          hasProperty("status", is(equalTo(TicketStatus.NOT_APPLICABLE))))));
        assertThat(tickets, hasItem(allOf(instanceOf(DoiRequest.class),
                                          hasProperty("status", is(equalTo(TicketStatus.NOT_APPLICABLE))))));
        assertThat(tickets, hasItem(allOf(instanceOf(UnpublishRequest.class),
                                          hasProperty("status", is(equalTo(TicketStatus.PENDING))))));
        assertThat(tickets, hasItem(allOf(instanceOf(PublishingRequestCase.class),
                                          hasProperty("status", is(equalTo(TicketStatus.COMPLETED))))));
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(UNPUBLISHED)));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, mode = Mode.EXCLUDE, names = {"NEW", "DRAFT_FOR_DELETION", "DELETED"})
    void shouldAllowPublish(PublicationStatus status) throws ApiGatewayException {
        var publication = randomPublication().copy().withStatus(status).build();
        resourceService.insertPreexistingPublication(publication);
        resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(PUBLISHED)));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, mode = Mode.EXCLUDE, names = {"DRAFT", "PUBLISHED_METADATA",
        "PUBLISHED", "DELETED", "UNPUBLISHED"})
    void shouldNotAllowPublish(PublicationStatus status) {
        var publication = randomPublication().copy().withStatus(status).build();
        resourceService.insertPreexistingPublication(publication);
        assertThrows(UnsupportedPublicationStatusTransition.class,
                     () -> resourceService.publishPublication(UserInstance.fromPublication(publication),
                                                              publication.getIdentifier()));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenUpdatingStatusWhenUpdatingPublication() throws BadRequestException {
        var publication = createPersistedPublicationWithDoi();
        publication.setStatus(PublicationStatus.PUBLISHED_METADATA);
        assertThrows(IllegalStateException.class, () -> resourceService.updatePublication(publication));
    }

    @Test
    void shouldDeleteUnpublishedPublication() throws ApiGatewayException {
        var publication = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(publication);
        resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        resourceService.unpublishPublication(resourceService.getPublication(publication), userInstance);
        resourceService.deletePublication(resourceService.getPublication(publication), userInstance);

        var deletedPublication = resourceService.getPublication(publication);
        assertThat(deletedPublication.getStatus(), is(equalTo(PublicationStatus.DELETED)));
    }

    @Test
    void shouldUpdatePublicationVersionWhenRefreshingResource() throws ApiGatewayException {
        var publication = createPublishedResource();
        var version = Resource.fromPublication(publication).toDao().getVersion();
        resourceService.refresh(publication.getIdentifier());
        var updatedPublication = resourceService.getPublication(publication);
        var updatesVersion = Resource.fromPublication(updatedPublication).toDao().getVersion();

        assertThat(updatesVersion, is(not(equalTo(version))));
    }

    @Test
    void shouldLogWhenPublicationToRefreshDoesNotExist() {
        var publication = randomPublication();
        var appender = LogUtils.getTestingAppender(ResourceService.class);
        resourceService.refresh(publication.getIdentifier());
        assertThat(appender.getMessages(), Matchers.containsString("Resource to refresh is not found"));
    }

    @Test
    void shouldThrowBadRequestWhenAttemptingToDeletePublishedPublication() throws ApiGatewayException {
        var publication = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(publication);
        resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        assertThrows(BadRequestException.class,
                     () -> resourceService.deletePublication(resourceService.getPublication(publication), userInstance));
    }

    @Test
    void shouldUpdatePublicationByImportedEntry() throws ApiGatewayException {
        var publication = createPublishedResource();
        publication.setDoi(randomDoi());
        var updatedPublication = resourceService.updatePublicationByImportEntry(publication,
                                                                   ImportSource.fromSource(Source.CRISTIN));

        assertThat(updatedPublication.getImportDetails().size(), is(equalTo(1)));
        assertThat(updatedPublication.getImportDetails().getFirst().importSource().getSource(),
                   is(equalTo(Source.CRISTIN)));
    }

    @Test
    void shouldPersistResourceCreatedEventWhenCreatingNewPublication() throws ApiGatewayException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication)
                           .persistNew(resourceService, userInstance);

        var resource = resourceService.getResourceByIdentifier(peristedPublication.getIdentifier());

        assertNotNull(resource.getResourceEvent());
    }

    @Test
    void shouldFetchResource() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication)
                                      .persistNew(resourceService, userInstance);

        var resource = Resource.resourceQueryObject(peristedPublication.getIdentifier())
                           .fetch(resourceService);

        assertEquals(peristedPublication, resource.orElseThrow().toPublication());
    }

    @Test
    void shouldRepublishResourceAndSetResourceEvent() throws ApiGatewayException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication)
                                      .persistNew(resourceService, userInstance);

        resourceService.publishPublication(userInstance, peristedPublication.getIdentifier());
        resourceService.unpublishPublication(peristedPublication, userInstance);
        Resource.resourceQueryObject(peristedPublication.getIdentifier())
            .fetch(resourceService)
            .orElseThrow()
            .republish(resourceService, userInstance);

        var republishedResource = Resource.resourceQueryObject(peristedPublication.getIdentifier())
                                      .fetch(resourceService).orElseThrow();

        assertEquals(PUBLISHED, republishedResource.getStatus());
        assertInstanceOf(RepublishedResourceEvent.class, republishedResource.getResourceEvent());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenRepublishingNotPublishedPublication() throws ApiGatewayException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication)
                                      .persistNew(resourceService, userInstance);

        assertThrows(IllegalStateException.class,
                     () -> Resource.resourceQueryObject(peristedPublication.getIdentifier())
                               .fetch(resourceService)
                               .orElseThrow()
                               .republish(resourceService, userInstance));
    }

    @Test
    void shouldSetResourceEventToNull() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication)
                                      .persistNew(resourceService, userInstance);

        var resource = Resource.resourceQueryObject(peristedPublication.getIdentifier())
                           .fetch(resourceService).orElseThrow();

        assertNotNull(resource.getResourceEvent());

        resource.clearResourceEvent(resourceService);

        assertNull(resource.getResourceEvent());
    }

    @Test
    void shouldNotPublishAlreadyPublishedPublication() throws ApiGatewayException {
        resourceService = mock(ResourceService.class);
        var publishedPublication = randomPublication().copy().withStatus(PUBLISHED).build();
        when(resourceService.getResourceByIdentifier(any())).thenReturn(Resource.fromPublication(publishedPublication));
        Resource.resourceQueryObject(publishedPublication.getIdentifier()).publish(resourceService,
                                                                                   UserInstance.fromPublication(publishedPublication));

        verify(resourceService, never()).updateResource(any());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenPublishingNotPublishableResource()
        throws BadRequestException, NotFoundException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication)
                                      .persistNew(resourceService, userInstance);
        var resource = Resource.fromPublication(peristedPublication).fetch(resourceService).orElseThrow();
        resource.setStatus(DRAFT_FOR_DELETION);
        resourceService.updateResource(resource);

        assertThrows(IllegalStateException.class,
                     () -> Resource.resourceQueryObject(
                         peristedPublication.getIdentifier()).publish(resourceService, userInstance));
    }

    @Test
    void shouldSetAllTicketsToNotApplicableWhenMarkingDraftPublicationAsDraftForDeletion() throws ApiGatewayException {
        var publication = createPersistedPublicationWithoutDoi();
        var ticket = TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class)
                         .withOwnerAffiliation(randomUri())
                         .withOwner(randomString())
                         .persistNewTicket(ticketService);
        resourceService.markPublicationForDeletion(UserInstance.fromPublication(publication), publication.getIdentifier());

        assertEquals(TicketStatus.NOT_APPLICABLE,ticket.fetch(ticketService).getStatus());
    }

    @Test
    void shouldPersistFileEntry() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        var persistedFile = fileEntry.fetch(resourceService);

        assertEquals(persistedFile.orElseThrow().getFile(), file);
    }

    @Test
    void shouldDeleteFile() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);
        fileEntry.delete(resourceService);

        assertEquals(Optional.empty(), fileEntry.fetch(resourceService));
    }

    @Test
    void shouldUpdateFile() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomHiddenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        var updatedFile = file.copy().withLicense(randomUri()).buildHiddenFile();
        fileEntry.update(updatedFile, resourceService);

        assertEquals(updatedFile, fileEntry.fetch(resourceService).orElseThrow().getFile());
    }

    @Test
    void shouldFetchPublicationFromFile() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        assertEquals(persistedPublication, fileEntry.toPublication(resourceService));
    }

    @Test
    void shouldCreateQueryObjectThatCanBeFetched() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);
        var persistedFileEntry = fileEntry.fetch(resourceService).orElseThrow();

        var fetchedQueryObject = FileEntry.queryObject(file.getIdentifier(), persistedPublication.getIdentifier())
                              .fetch(resourceService)
                              .orElseThrow();

        assertEquals(persistedFileEntry, fetchedQueryObject);
    }

    @Test
    void shouldFetchResourceWithFiles() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        var resource = Resource.fromPublication(persistedPublication).fetchResourceWithFiles(resourceService);

        assertTrue(resource.orElseThrow().getAssociatedArtifacts().contains(file));
    }

    @Test
    void shouldRejectPersistedFile() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        fileEntry.reject(resourceService);

        var rejectedFileEntry = fileEntry.fetch(resourceService).orElseThrow();

        assertInstanceOf(RejectedFile.class, rejectedFileEntry.getFile());
    }

    @Test
    void shouldApprovePersistedFile() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        fileEntry.approve(resourceService);

        var rejectedFileEntry = fileEntry.fetch(resourceService).orElseThrow();

        assertInstanceOf(OpenFile.class, rejectedFileEntry.getFile());
    }

    @Test
    void shouldFetchResourceWithNewFilesWhenEnvironmentVariableShouldUseNewFileIsSet() throws BadRequestException {
        var environment = mock(Environment.class);
        when(environment.readEnvOpt("SHOULD_USE_NEW_FILES")).thenReturn(Optional.of("Yes"));
        var resourceService = getResourceServiceBuilder(client)
                                  .withEnvironment(environment)
                                  .build();
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        FileEntry.create(file, resourceIdentifier, userInstance).persist(resourceService);

        var resource = Resource.fromPublication(persistedPublication).fetch(resourceService).orElseThrow();

        assertTrue(resource.getAssociatedArtifacts().contains(file));
    }

    @Test
    void shouldApproveApprovedFilesWhenShouldUseNewFilesIsPresent() throws ApiGatewayException {
        var environment = mock(Environment.class);
        when(environment.readEnvOpt("SHOULD_USE_NEW_FILES")).thenReturn(Optional.of("Yes"));
        var resourceService = getResourceServiceBuilder().withEnvironment(environment).build();

        var publication = randomPublication().copy().withAssociatedArtifacts(new ArrayList<>()).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingInternalFile();
        FileEntry.create(file, persistedPublication.getIdentifier(), userInstance).persist(resourceService);

        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.fromPublication(persistedPublication)
                                                            .withFilesForApproval(Set.of(file))
                                                            .withOwner(randomString())
                                                            .persistNewTicket(ticketService);

        publishingRequest.publishApprovedFile().persistUpdate(ticketService);
        publishingRequest.publishApprovedFiles(resourceService);



        assertInstanceOf(InternalFile.class, FileEntry.queryObject(file.getIdentifier(), persistedPublication.getIdentifier())
                                                 .fetch(resourceService)
                           .orElseThrow()
                           .getFile());
    }

    @Test
    void shouldApproveApprovedFilesWhenFilesAreInAssociatedArtifacts() throws ApiGatewayException {
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);


        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.fromPublication(persistedPublication)
                                                            .withFilesForApproval(Set.of(file))
                                                            .withOwner(randomString())
                                                            .persistNewTicket(ticketService);
        publishingRequest.publishApprovedFile().persistUpdate(ticketService);

        publishingRequest.publishApprovedFiles(resourceService);

        var associatedArtifact = Resource.fromPublication(persistedPublication)
                                     .fetch(resourceService).orElseThrow().getAssociatedArtifacts().getFirst();
        assertInstanceOf(InternalFile.class, associatedArtifact);
    }

    @Test
    void shouldRejectRejectedFilesWhenShouldUseNewFilesIsPresent() throws ApiGatewayException {
        var environment = mock(Environment.class);
        when(environment.readEnvOpt("SHOULD_USE_NEW_FILES")).thenReturn(Optional.of("Yes"));
        var resourceService = getResourceServiceBuilder().withEnvironment(environment).build();

        var publication = randomPublication().copy().withAssociatedArtifacts(new ArrayList<>()).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingInternalFile();
        FileEntry.create(file, persistedPublication.getIdentifier(), userInstance).persist(resourceService);

        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.fromPublication(persistedPublication)
                                                            .withFilesForApproval(Set.of(file))
                                                            .withOwner(randomString())
                                                            .persistNewTicket(ticketService);

        publishingRequest.rejectRejectedFiles(resourceService);



        assertInstanceOf(RejectedFile.class, FileEntry.queryObject(file.getIdentifier(), persistedPublication.getIdentifier())
                                                 .fetch(resourceService)
                                                 .orElseThrow()
                                                 .getFile());
    }

    @Test
    void shouldRejectRejectedFilesWhenFilesAreInAssociatedArtifacts() throws ApiGatewayException {
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);


        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.fromPublication(persistedPublication)
                                                            .withFilesForApproval(Set.of(file))
                                                            .withOwner(randomString())
                                                            .persistNewTicket(ticketService);
        publishingRequest.publishApprovedFile().persistUpdate(ticketService);

        publishingRequest.rejectRejectedFiles(resourceService);

        var associatedArtifact = Resource.fromPublication(persistedPublication)
                                     .fetch(resourceService).orElseThrow().getAssociatedArtifacts().getFirst();

        assertInstanceOf(RejectedFile.class, associatedArtifact);
    }

    @Test
    void shouldMigrateFilesToFileEntriesAndPersistDatabaseEntryForEachFileWithTheSameUserInstanceAsPublicationOwner()
        throws BadRequestException {
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var queryObject = FileEntry.queryObject(file.getIdentifier(), persistedPublication.getIdentifier());

        assertTrue(queryObject.fetch(resourceService).isEmpty());

        resourceService.refreshResources(List.of(Resource.fromPublication(persistedPublication)));

        var persistedFileEntry = queryObject.fetch(resourceService);

        assertTrue(persistedFileEntry.isPresent());

        var fileEntry = persistedFileEntry.orElseThrow();
        var userInstanceFromPersistedFile = UserInstance.create(fileEntry.getOwner().toString(), fileEntry.getCustomerId(),
                                                                null, List.of(), fileEntry.getOwnerAffiliation());
        assertEquals(userInstance, userInstanceFromPersistedFile);
    }

    @Test
    void shouldRemoveFileFromAssociatedArtifactsWhenFileIsPresentInBothDatabaseAndAssociatedArtifacts()
        throws BadRequestException, NotFoundException {
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        FileEntry.create(file, persistedPublication.getIdentifier(), userInstance).persist(resourceService);

        resourceService.refreshResources(List.of(Resource.fromPublication(persistedPublication)));

        var migratedResourceDao = (ResourceDao) ResourceDao.queryObject(userInstance, persistedPublication.getIdentifier())
                                                    .fetchByIdentifier(client, DatabaseConstants.RESOURCES_TABLE_NAME);
        var migratedResource = migratedResourceDao.getResource();

        assertFalse( migratedResource.getAssociatedArtifacts().contains(file));
    }

    @Test
    void shouldRemoveFileMetadataFromAssociatedArtifactsOnceItHasBeenMigrated()
        throws BadRequestException, NotFoundException {
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        resourceService.refreshResources(List.of(Resource.fromPublication(persistedPublication)));

        var migratedResourceDao = (ResourceDao) ResourceDao.queryObject(userInstance, persistedPublication.getIdentifier())
                         .fetchByIdentifier(client, DatabaseConstants.RESOURCES_TABLE_NAME);
        var migratedResource = migratedResourceDao.getResource();

        assertFalse( migratedResource.getAssociatedArtifacts().contains(file));
    }

    @Test
    void shouldKeepAssociatedLinkWhenMigratingFiles()
        throws BadRequestException, NotFoundException {
        var associatedLink = new AssociatedLink(randomUri(), randomString(), randomString());
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(associatedLink, file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        resourceService.refreshResources(List.of(Resource.fromPublication(persistedPublication)));

        var migratedResourceDao = (ResourceDao) ResourceDao.queryObject(userInstance, persistedPublication.getIdentifier())
                                                    .fetchByIdentifier(client, DatabaseConstants.RESOURCES_TABLE_NAME);
        var migratedResource = migratedResourceDao.getResource();

        assertTrue( migratedResource.getAssociatedArtifacts().contains(associatedLink));
        assertFalse( migratedResource.getAssociatedArtifacts().contains(file));
    }

    private static AssociatedArtifactList createEmptyArtifactList() {
        return new AssociatedArtifactList(emptyList());
    }

    private ResourceService getResourceServiceWithDuplicateIdentifier(SortableIdentifier identifier) {
        return getResourceServiceBuilder(client)
                   .withIdentifierSupplier(() -> identifier)
                   .withUriRetriever(mock(UriRetriever.class))
                   .build();
    }

    private Username randomPerson() {
        return new Username(randomString());
    }

    private Set<Publication> createSamplePublicationsOfSingleCristinIdentifier(String cristinIdentifier) {
        UserInstance userInstance = UserInstance.create(randomString(), randomUri());
        return Stream.of(publicationWithIdentifier(), publicationWithIdentifier(), publicationWithIdentifier())
                   .map(publication -> injectOwner(userInstance, publication))
                   .map(publication -> injectCristinIdentifier(cristinIdentifier, publication))
                   .map(attempt(res -> createPersistedPublicationWithDoi(resourceService, res)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toSet());
    }

    private Publication injectCristinIdentifier(String cristinIdentifier, Publication publication) {
        publication.setAdditionalIdentifiers(Set.of(new CristinIdentifier(SourceName.fromCristin("ntnu@123"),
                                                                          cristinIdentifier)));
        return publication;
    }

    private void assertThatFailedBatchScanLogsProperly(TestAppender testAppender, Set<Publication> userResources) {
        assertThat(testAppender.getMessages(), containsString("AmazonDynamoDBException"));
        userResources.forEach(publication -> {
            var expected = "Resource:" + publication.getIdentifier().toString();
            assertThat(testAppender.getMessages(), containsString(expected));
        });
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withStatus(PublicationStatus.PUBLISHED)
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withLink(randomUri())
                   .withDoi(randomDoi())
                   .withHandle(randomUri())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                   .withFundings(List.of())
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .withEntityDescription(randomEntityDescription(JournalArticle.class))
                   .build();
    }

    private Publication createPersistedPublicationWithManyContributions(int amount) throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();
        var contributions = IntStream
                                .rangeClosed(1, amount)
                                .mapToObj(i -> randomContributor())
                                .toList();
        publication.getEntityDescription().setContributors(contributions);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication createPersistedPublicationWithManyContributionsWithoutAffiliations(int amount)
        throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();
        var contributions = IntStream
                                .rangeClosed(1, amount)
                                .mapToObj(i -> randomContributor(List.of()))
                                .toList();
        publication.getEntityDescription().setContributors(contributions);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Contributor randomContributor() {
        return randomContributor(List.of(randomOrganization()));
    }

    private Contributor randomContributor(List<Corporation> affiliations) {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withSequence(randomInteger(10000))
                   .withAffiliations(affiliations)
                   .withIdentity(randomIdentity())
                   .build();
    }

    private Identity randomIdentity() {
        return new Identity.Builder().withName(randomString()).withOrcId(randomString()).withId(
            RandomPersonServiceResponse.randomUri()).build();
    }

    private Publication draftPublicationWithoutDoiAndAssociatedLink() {

        return randomPublication().copy()
                   .withDoi(null)
                   .withStatus(DRAFT)
                   .withAssociatedArtifacts(List.of(randomAssociatedLink()))
                   .build();
    }

    private Publication createPersistedPublicationWithoutDoi() throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private Publication createPersistedPublicationWithoutDoi(Publication publication) throws BadRequestException {
        var withoutDoi = publication.copy().withDoi(null).build();
        return Resource.fromPublication(withoutDoi).persistNew(resourceService,
                                                               UserInstance.fromPublication(withoutDoi));
    }

    private Publication createPersistedPublicationWithDoi(ResourceService resourceService, Publication sampleResource)
        throws BadRequestException {
        return Resource.fromPublication(sampleResource).persistNew(resourceService,
                                                                   UserInstance.fromPublication(sampleResource));
    }

    private Publication createPersistedPublicationWithDoi() throws BadRequestException {
        return createPersistedPublicationWithDoi(resourceService, randomPublication());
    }

    private Publication createUnpublishablePublication() throws BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private Publication generatePublication() {
        return PublicationGenerator.publicationWithoutIdentifier().copy().withCuratingInstitutions(null).build();
    }

    private ListingResult<Entity> fetchRestOfDatabaseEntries(ListingResult<Entity> listingResult) {
        return resourceService.scanResources(BIG_PAGE, listingResult.getStartMarker(), Collections.emptyList());
    }

    private SortableIdentifier extractIdentifierFromFirstScanResult(ListingResult<Entity> listingResult) {
        return listingResult.getDatabaseEntries().stream().collect(SingletonCollector.collect()).getIdentifier();
    }

    private ListingResult<Entity> fetchFirstDataEntry() {
        ListingResult<Entity> listingResult = ListingResult.empty();
        while (listingResult.isEmpty()) {
            listingResult = resourceService.scanResources(1, listingResult.getStartMarker(), Collections.emptyList());
        }
        return listingResult;
    }

    private Publication publishResource(Publication resource) throws ApiGatewayException {
        resourceService.publishPublication(UserInstance.fromPublication(resource), resource.getIdentifier());
        return resourceService.getPublication(resource);
    }

    private void assertThatIdentifierEntryHasBeenCreated() {
        assertThatResourceAndIdentifierEntryExist();
    }

    private void assertThatResourceAndIdentifierEntryExist() {
        ScanResult result = client.scan(new ScanRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME));
        assertThat(result.getCount(), is(equalTo(2)));
    }

    private void assertThatTheEntriesHaveNotBeenDeleted() {
        assertThatResourceAndIdentifierEntryExist();
    }

    private void assertThatAllEntriesHaveBeenDeleted() {
        ScanResult result = client.scan(new ScanRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME));
        assertThat(result.getCount(), is(equalTo(0)));
    }

    private DoiRequest expectedDoiRequestAfterPublicationUpdate(Publication initialPublication,
                                                                DoiRequest initialDoiRequest,
                                                                Publication publicationUpdate,
                                                                DoiRequest updatedDoiRequest) {

        return DoiRequest.builder()
                   .withOwner(new User(initialPublication.getResourceOwner().getOwner().getValue()))
                   .withCustomerId(initialPublication.getPublisher().getId())
                   .withIdentifier(initialDoiRequest.getIdentifier())
                   .withCreatedDate(initialDoiRequest.getCreatedDate())
                   .withModifiedDate(updatedDoiRequest.getModifiedDate())
                   .withStatus(TicketStatus.PENDING)
                   .withResourceStatus(publicationUpdate.getStatus())
                   .withResourceIdentifier(publicationUpdate.getIdentifier())
                   .withViewedBy(initialDoiRequest.getViewedBy())
                   .build();
    }

    private Publication updateAllPublicationFieldsExpectIdentifierStatusAndOwnerInfo(Publication existingPublication) {
        return randomPublication().copy()
                   .withIdentifier(existingPublication.getIdentifier())
                   .withPublisher(existingPublication.getPublisher())
                   .withResourceOwner(existingPublication.getResourceOwner())
                   .withStatus(existingPublication.getStatus())
                   .build();
    }

    private DoiRequest createDoiRequest(Publication resource) throws ApiGatewayException {
        return (DoiRequest) DoiRequest.fromPublication(resource).persistNewTicket(ticketService);
    }

    private void verifyThatTheResourceIsInThePublishedResources(Publication resourceWithStatusDraft) {
        ResourceDao resourceDaoWithStatusPublished = queryObjectForPublishedResource(resourceWithStatusDraft);

        Optional<ResourceDao> publishedResource = searchForResource(resourceDaoWithStatusPublished);
        assertThat(publishedResource.isPresent(), is(true));

        var actualResourceDao = publishedResource.orElseThrow();
        var resource = (Resource) actualResourceDao.getData();
        assertThat(resource.getStatus(), is(equalTo(PUBLISHED)));
    }

    private void verifyThatTheResourceWasMovedFromTheDrafts(ResourceDao resourceDaoWithStatusDraft) {
        Optional<ResourceDao> expectedEmptyResult = searchForResource(resourceDaoWithStatusDraft);
        assertThat(expectedEmptyResult.isEmpty(), is(true));
    }

    private void assertThatResourceCanBeFoundInDraftResources(ResourceDao resourceDaoWithStatusDraft) {
        Optional<ResourceDao> savedResource = searchForResource(resourceDaoWithStatusDraft);
        assertThat(savedResource.isPresent(), is(true));
    }

    private ResourceDao queryObjectForPublishedResource(Publication resourceWithStatusDraft) {
        Resource resourceWithStatusPublished = Resource.fromPublication(resourceWithStatusDraft)
                                                   .copy()
                                                   .withStatus(PUBLISHED)
                                                   .build();
        return new ResourceDao(resourceWithStatusPublished);
    }

    private QueryResult queryForDraftResource(ResourceDao resourceDao) {

        return client.query(new QueryRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                                .withIndexName(DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
                                .withKeyConditions(resourceDao.fetchEntryByTypeCustomerStatusKey()));
    }

    private Optional<ResourceDao> parseResult(QueryResult result) {
        return result.getItems().stream().map(map -> parseAttributeValuesMap(map, ResourceDao.class)).findAny();
    }

    private Publication createPublishedResource() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithoutDoi();
        publishResource(resource);
        return resourceService.getPublication(resource);
    }

    private void assertThatJsonProcessingErrorIsPropagatedUp(Class<JsonProcessingException> expectedExceptionClass,
                                                             Executable action) {
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(expectedExceptionClass)));
    }

    private Set<Publication> createSamplePublicationsOfSingleOwner(UserInstance userInstance) {
        return Stream.of(publicationWithIdentifier(), publicationWithIdentifier(), publicationWithIdentifier())
                   .map(publication -> injectOwner(userInstance, publication))
                   .map(attempt(res -> createPersistedPublicationWithDoi(resourceService, res)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toSet());
    }

    private Publication createPublicationWithStatus(UserInstance userInstance, PublicationStatus status) {
        var publication = injectOwner(userInstance, publicationWithIdentifier());
        publicationWithIdentifier().setStatus(status);
        return attempt(() -> createPersistedPublicationWithDoi(resourceService, publication)).orElseThrow();
    }

    private Publication injectOwner(UserInstance userInstance, Publication publication) {
        return publication.copy()
                   .withResourceOwner(new ResourceOwner(new Username(userInstance.getUsername()),
                                                        AFFILIATION_NOT_IMPORTANT))
                   .withPublisher(new Organization.Builder().withId(userInstance.getCustomerId()).build())
                   .build();
    }

    private Publication expectedUpdatedResource(Publication originalResource, Publication updatedResource,
                                                UserInstance expectedOwner) {
        return originalResource.copy()
                   .withResourceOwner(new ResourceOwner(new Username(expectedOwner.getUsername()),
                                                        AFFILIATION_NOT_IMPORTANT))
                   .withPublisher(userOrganization(expectedOwner))
                   .withCreatedDate(originalResource.getCreatedDate())
                   .withModifiedDate(updatedResource.getModifiedDate())
                   .build();
    }

    private void assertThatUpdateFails(Publication resourceUpdate) {
        Executable action = () -> resourceService.updatePublication(resourceUpdate);
        assertThrows(TransactionFailedException.class, action);
    }

    private Publication updateResourceTitle(Publication resource) {
        EntityDescription newEntityDescription = new EntityDescription.Builder().withMainTitle(UPDATED_TITLE).build();

        assertThatNewEntityDescriptionDiffersOnlyInTitle(resource.getEntityDescription(), newEntityDescription);
        return resource.copy().withEntityDescription(newEntityDescription).build();
    }

    private void assertThatNewEntityDescriptionDiffersOnlyInTitle(EntityDescription oldEntityDescription,
                                                                  EntityDescription newEntityDescription) {
        String mainTitleFieldName = fetchMainTitleFieldName();
        Diff diff = JAVERS.compare(oldEntityDescription, newEntityDescription);
        int mainTitleChanges = diff.getPropertyChanges(mainTitleFieldName).size();
        assertThat(mainTitleChanges, is(equalTo(1)));
    }

    private String fetchMainTitleFieldName() {
        return attempt(() -> EntityDescription.class.getDeclaredField(MAIN_TITLE_FIELD).getName()).orElseThrow(
            fail -> new RuntimeException(ENTITY_DESCRIPTION_DOES_NOT_HAVE_FIELD_ERROR));
    }

    private void assertThatResourceDoesNotExist(Publication sampleResource) {
        assertThrows(NotFoundException.class, () -> resourceService.getPublication(sampleResource));
    }

    private UserInstance someOtherUser() {
        return UserInstance.create(SOME_OTHER_USER, SOME_OTHER_ORG);
    }

    private Organization anotherPublisher() {
        return new Organization.Builder().withId(SOME_OTHER_ORG).build();
    }

    private Organization newOrganization() {
        return new Organization.Builder().withId(ResourceServiceTest.SOME_OTHER_ORG).build();
    }
}