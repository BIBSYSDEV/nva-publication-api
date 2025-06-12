package no.unit.nva.publication.service.impl;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static java.util.Collections.emptyList;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.UriRetriever;
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
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.model.business.logentry.LogOrganization;
import no.unit.nva.publication.model.business.publicationstate.CreatedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.FileDeletedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileHiddenEvent;
import no.unit.nva.publication.model.business.publicationstate.FileImportedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileRejectedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileRetractedEvent;
import no.unit.nva.publication.model.business.publicationstate.ImportedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.MergedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.RepublishedResourceEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
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
    private static final String ENTITY_DESCRIPTION_DOES_NOT_HAVE_FIELD_ERROR =
        EntityDescription.class.getName() + " does not have a field" + MAIN_TITLE_FIELD;
    private static final Javers JAVERS = JaversBuilder.javers().build();
    private static final int BIG_PAGE = 10;
    private static final URI UNIMPORTANT_AFFILIATION = null;
    private static final URI AFFILIATION_NOT_IMPORTANT = null;
    private static final URI SOME_ORG = randomUri();
    private static final UserInstance SAMPLE_USER = UserInstance.create(randomString(), SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
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
        var resourceService = getResourceServiceBuilder(client).withTableName(customTable).build();
        List<String> tableNames = resourceService.getClient().listTables().getTableNames();
        assertThat(tableNames, hasItem(customTable));
    }

    @Test
    void shouldSetImportedEntryCreationModifiedAndPublishedDates() throws NotFoundException {
        var startOfTest = Instant.now();
        var inputPublication = randomPublication().copy().withCuratingInstitutions(null).withStatus(PUBLISHED).build();
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
        var readResource = resourceService.getResourceByIdentifier(savedResource.getIdentifier()).toPublication();
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
                                                  .withResourceOwner(
                                                      new ResourceOwner(new Username(SOME_OTHER_USER), null))
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
        Publication savedResource = resourceService.getPublicationByIdentifier(sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }

    @Test
    void whenPublicationOwnerIsUpdatedTheResourceEntryMaintainsTheRestResourceMetadata() throws ApiGatewayException {
        var originalResource = createPersistedPublicationWithDoi();

        var oldOwner = UserInstance.fromPublication(originalResource);
        var newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);

        assertThatResourceHaveNewOwner(originalResource);

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

        assertThatResourceHaveNewOwner(sampleResource);

        Publication newResource = resourceService.getPublicationByIdentifier(sampleResource.getIdentifier());

        assertThat(newResource.getModifiedDate(), is(greaterThan(newResource.getCreatedDate())));
    }

    @Test
    void resourceIsUpdatedWhenResourceUpdateIsReceived() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        Publication actualOriginalResource = resourceService.getPublicationByIdentifier(resource.getIdentifier());
        assertThat(actualOriginalResource, is(equalTo(resource)));

        Publication resourceUpdate = updateResourceTitle(resource);
        resourceService.updatePublication(resourceUpdate);
        Publication actualUpdatedResource = resourceService.getPublicationByIdentifier(resource.getIdentifier());

        assertThat(actualUpdatedResource.getEntityDescription().getMainTitle(),
                   is(equalTo(resourceUpdate.getEntityDescription().getMainTitle())));
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
        when(client.query(any(QueryRequest.class))).thenThrow(expectedMessage);
        var resource = publicationWithIdentifier();

        var failingResourceService = getResourceServiceBuilder(client).build();

        Executable action = () -> failingResourceService.getPublicationByIdentifier(resource.getIdentifier());
        var exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), is(equalTo(expectedMessage.getMessage())));
    }

    @ParameterizedTest(name = "Should return publication by owner when status is {0}")
    @EnumSource(value = PublicationStatus.class, mode = Mode.EXCLUDE, names = {"DRAFT_FOR_DELETION"})
    void shouldReturnPublicationsForOwnerWithStatus(PublicationStatus status) {
        var userInstance = randomUserInstance();
        var publication = createPublicationWithStatus(userInstance, status);
        var actualResources = resourceService.getPublicationSummaryByOwner(userInstance);
        var resourceSet = new HashSet<>(actualResources);
        assertThat(resourceSet, containsInAnyOrder(PublicationSummary.create(publication)));
    }

    @Test
    void getResourcesByOwnerReturnsAllResourcesOwnedByUser() {
        UserInstance userInstance = randomUserInstance();
        Set<PublicationSummary> userResources = createSamplePublicationsOfSingleOwner(userInstance).stream()
                                                    .map(PublicationSummary::create)
                                                    .collect(Collectors.toSet());

        List<PublicationSummary> actualResources = resourceService.getPublicationSummaryByOwner(userInstance);
        HashSet<PublicationSummary> actualResourcesSet = new HashSet<>(actualResources);

        assertThat(actualResourcesSet, containsInAnyOrder(userResources.toArray(PublicationSummary[]::new)));
    }

    @Test
    void getResourcesByCristinIdentifierReturnsAllResourcesWithCristinIdentifier() {
        String cristinIdentifier = randomString();
        persistSamplePublicationsOfSingleCristinIdentifier(cristinIdentifier);
        List<Publication> actualPublication = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);
        HashSet<Publication> actualResourcesSet = new HashSet<>(actualPublication);
        assertTrue(actualPublication.containsAll(actualResourcesSet));
    }

    @Test
    @Disabled
        //TODO: Decide how to manage multiple cristin identifiers that exist in the data
    void combinationOfTrustedAndUntrustedCristinIdentifiersIsNotAllowed()
        throws BadRequestException, NotFoundException {

        Publication resource = createPersistedPublicationWithDoi();
        Publication actualOriginalResource = resourceService.getPublicationByIdentifier(resource.getIdentifier());
        assertThat(actualOriginalResource, is(equalTo(resource)));

        Set<AdditionalIdentifierBase> cristinIdentifiers = Set.of(new AdditionalIdentifier("Cristin", randomString()),
                                                                  new CristinIdentifier(SourceName.fromBrage("uit"),
                                                                                        randomString()));
        var updatedResource = resource.copy().withAdditionalIdentifiers(cristinIdentifiers).build();

        assertThrows(IllegalArgumentException.class, () -> resourceService.updatePublication(updatedResource));
    }

    @Test
    void getResourcesByOwnerReturnsEmptyListWhenUseHasNoPublications() {

        List<PublicationSummary> actualResources = resourceService.getPublicationSummaryByOwner(SAMPLE_USER);
        HashSet<PublicationSummary> actualResourcesSet = new HashSet<>(actualResources);

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
                                                  () -> failingResourceService.getPublicationSummaryByOwner(
                                                      SAMPLE_USER));

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
                                                    () -> failingResourceService.getPublicationSummaryByOwner(
                                                        SAMPLE_USER));
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
        var publishedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertEquals(PUBLISHED, publishedPublication.getStatus());
        assertNotNull(publishedPublication.getPublishedDate());
    }

    @Test
    void byTypeCustomerStatusIndexIsUpdatedWhenResourceIsUpdated() throws ApiGatewayException {
        Publication resourceWithStatusDraft = createPersistedPublicationWithDoi();
        ResourceDao resourceDaoWithStatusDraft = new ResourceDao(Resource.fromPublication(resourceWithStatusDraft));

        assertThatResourceCanBeFoundInDraftResources(resourceDaoWithStatusDraft);

        Resource.fromPublication(resourceWithStatusDraft)
            .publish(resourceService, UserInstance.fromPublication(resourceWithStatusDraft));

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

        Executable action = () -> Resource.fromPublication(savedResource)
                                      .publish(resourceService, UserInstance.fromPublication(sampleResource));

        assertThrows(IllegalStateException.class, action);
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
        var userInstance = randomUserInstance();
        Executable action = () -> resourceService.markPublicationForDeletion(userInstance, resource.getIdentifier());
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE));
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

        Publication resourceForDeletion = resourceService.getPublicationByIdentifier(resource.getIdentifier());
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
        Publication actualResource = resourceService.getPublicationByIdentifier(resource.getIdentifier());
        assertThat(actualResource.getStatus(), is(equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        Executable action = () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                                                                             resource.getIdentifier());
        assertThrows(BadRequestException.class, action);
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
    void deleteDraftPublicationDeletesDraftResource() throws ApiGatewayException {
        var publication = createPersistedPublicationWithDoi();
        assertThatIdentifierEntryHasBeenCreated();

        Executable fetchResourceAction = () -> resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertDoesNotThrow(fetchResourceAction);

        var userInstance = UserInstance.fromPublication(publication);
        resourceService.deleteDraftPublication(userInstance, publication.getIdentifier());
        assertThrows(NotFoundException.class, fetchResourceAction);

        assertThatAllEntriesHaveBeenDeleted();
    }

    @Test
    void deleteDraftPublicationThrowsExceptionWhenResourceIsPublished() throws ApiGatewayException {
        var publication = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(publication);
        Resource.fromPublication(publication).publish(resourceService, userInstance);
        assertThatIdentifierEntryHasBeenCreated();

        Executable fetchResourceAction = () -> resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertDoesNotThrow(fetchResourceAction);

        Executable deleteAction = () -> resourceService.deleteDraftPublication(userInstance,
                                                                               publication.getIdentifier());
        assertThrows(TransactionFailedException.class, deleteAction);

        assertThatTheEntriesHaveNotBeenDeleted();
    }

    @Test
    void deleteDraftPublicationDeletesDoiRequestWhenPublicationHasDoiRequest() throws ApiGatewayException {
        var publication = createPersistedPublicationWithoutDoi();
        createDoiRequest(publication);

        var userInstance = UserInstance.fromPublication(publication);
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
    void shouldScanEntriesInDatabaseAfterSpecifiedMarker(Class<? extends TicketEntry> ticketType,
                                                         PublicationStatus status) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithAssociatedLink(status, resourceService);
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

        assertThrows(IllegalStateException.class,
                     () -> Resource.fromPublication(samplePublication).publish(resourceService, userInstance));
    }

    @Test
    void shouldMaintainAssociatedArtifactsThatOtherThanFile() throws ApiGatewayException {
        var publication = draftPublicationWithoutDoiAndAssociatedLink();
        var persistedDraft = Resource.fromPublication(publication)
                                 .persistNew(resourceService, UserInstance.fromPublication(publication));

        Resource.fromPublication(persistedDraft).publish(resourceService, UserInstance.fromPublication(persistedDraft));
        var persistedPublished = resourceService.getPublicationByIdentifier(persistedDraft.getIdentifier());
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
        var actualPublicationInDatabaseAfterStatusUpdate = resourceService.getPublicationByIdentifier(
            publicationIdentifier);
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
    void shouldNotUpdatePublicationCuratingInstitutionsWhenContributorsAreUnchanged() throws ApiGatewayException {
        var publication = randomPublication();
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

        var affiliation = (new Organization.Builder()).withId(orgId).build();

        publication.getEntityDescription().setContributors(List.of(randomContributor(List.of(affiliation))));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(topLevelId, Set.of(randomUri()))));
        publication.setAssociatedArtifacts(AssociatedArtifactList.empty());
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var updatedResource = resourceService.updatePublication(resource);

        verify(uriRetriever, never()).getRawContent(eq(orgId), any());
        assertThat(updatedResource.getCuratingInstitutions().stream().findFirst().orElseThrow().id(),
                   is(equalTo(topLevelId)));
    }

    @Test
    void shouldNotSetCuratingInstitutionsWhenUpdatingImportCandidateWhenContributorsAreUnchanged()
        throws ApiGatewayException {
        var importCandidate = randomImportCandidate();
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

        var affiliation = (new Organization.Builder()).withId(orgId).build();

        importCandidate.getEntityDescription().setContributors(List.of(randomContributor(List.of(affiliation))));
        importCandidate.setCuratingInstitutions(Set.of(new CuratingInstitution(topLevelId, Set.of(randomUri()))));

        var persistedImportCandidate = resourceService.persistImportCandidate(importCandidate);
        persistedImportCandidate.setAssociatedArtifacts(AssociatedArtifactList.empty());

        var updatedImportCandidate = resourceService.updateImportCandidate(persistedImportCandidate);

        verify(uriRetriever, never()).getRawContent(eq(orgId), any());
        assertThat(updatedImportCandidate.getCuratingInstitutions().stream().findFirst().orElseThrow().id(),
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
        var result = Resource.fromPublication(publication)
                         .persistNew(resourceService, UserInstance.fromPublication(publication));
        var storedPublication = resourceService.getPublicationByIdentifier(result.getIdentifier());
        assertThat(storedPublication.getPublicationNotes(), is(equalTo(publication.getPublicationNotes())));
    }

    @Test
    void shouldLogIdentifiersOfRecordsWhenBatchScanWriteFails() {
        var failingClient = new FailingDynamoClient(this.client);
        resourceService = getResourceServiceBuilder(failingClient).build();

        var userInstance = randomUserInstance();
        var userResources = createSamplePublicationsOfSingleOwner(userInstance);
        // correctness of this test rely on number of publications generated above does not exceed
        // ResourceService.MAX_SIZE_OF_BATCH_REQUEST
        var resources = userResources.stream().map(Resource::fromPublication).map(Entity.class::cast).toList();

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
        assertThat(resourceService.getPublicationByIdentifier(publication.getIdentifier()).getStatus(),
                   is(equalTo(UNPUBLISHED)));
    }

    @Test
    void shouldSetAllPendingTicketsToNotApplicableWhenUnpublishingPublication() throws ApiGatewayException {
        var publication = createPublishedResource();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication);
        createTickets(resource, userInstance);
        resourceService.unpublishPublication(publication, userInstance);
        var tickets = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication)).toList();
        assertThat(tickets, hasSize(5));
        assertThat(tickets, hasItem(
            allOf(instanceOf(GeneralSupportRequest.class), hasProperty("status", is(equalTo(TicketStatus.CLOSED))))));
        assertThat(tickets, hasItem(allOf(instanceOf(GeneralSupportRequest.class),
                                          hasProperty("status", is(equalTo(TicketStatus.NOT_APPLICABLE))))));
        assertThat(tickets, hasItem(
            allOf(instanceOf(DoiRequest.class), hasProperty("status", is(equalTo(TicketStatus.NOT_APPLICABLE))))));
        assertThat(tickets, hasItem(allOf(instanceOf(PublishingRequestCase.class),
                                          hasProperty("status", is(equalTo(TicketStatus.COMPLETED))))));
        assertThat(tickets, hasItem(allOf(instanceOf(PublishingRequestCase.class),
                                          hasProperty("status", is(equalTo(TicketStatus.NOT_APPLICABLE))))));
        assertThat(resourceService.getPublicationByIdentifier(publication.getIdentifier()).getStatus(),
                   is(equalTo(UNPUBLISHED)));
    }

    @Test
    void shouldSetAllNotApplicableTicketsToPendingWhenRepublishingPublication() throws ApiGatewayException {
        var publication = createPublishedResource();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication);

        createTickets(resource, userInstance);

        resourceService.unpublishPublication(publication, userInstance);
        resource.republish(resourceService, ticketService, userInstance);


        var tickets = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication)).toList();
        assertThat(tickets, hasSize(5));
        assertThat(tickets, hasItem(
            allOf(instanceOf(GeneralSupportRequest.class), hasProperty("status", is(equalTo(TicketStatus.PENDING))))));
        assertThat(tickets, hasItem(allOf(instanceOf(GeneralSupportRequest.class),
                                          hasProperty("status", is(equalTo(TicketStatus.PENDING))))));
        assertThat(tickets, hasItem(
            allOf(instanceOf(DoiRequest.class), hasProperty("status", is(equalTo(TicketStatus.PENDING))))));
        assertThat(tickets, hasItem(allOf(instanceOf(PublishingRequestCase.class),
                                          hasProperty("status", is(equalTo(TicketStatus.COMPLETED))))));
        assertThat(tickets, hasItem(allOf(instanceOf(PublishingRequestCase.class),
                                          hasProperty("status", is(equalTo(TicketStatus.PENDING))))));
        assertThat(resourceService.getPublicationByIdentifier(publication.getIdentifier()).getStatus(),
                   is(equalTo(PUBLISHED)));
    }

    @Test
    void shouldFetchAllFileEntries() throws ApiGatewayException {
        var publication = createPublishedResource();

        var actualNumberOfFiles = Resource.fromPublication(publication).fetchFileEntries(resourceService).count();

        var expectedNumberOfFiles = publication.getAssociatedArtifacts().stream()
                                        .filter(File.class::isInstance)
                                        .count();
        assertThat(actualNumberOfFiles, is(equalTo(expectedNumberOfFiles)));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, mode = Mode.EXCLUDE, names = {"NEW", "DRAFT_FOR_DELETION", "DELETED"})
    void shouldAllowPublish(PublicationStatus status) throws ApiGatewayException {
        var publication = randomPublication().copy().withStatus(status).build();
        resourceService.insertPreexistingPublication(publication);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        assertThat(resourceService.getPublicationByIdentifier(publication.getIdentifier()).getStatus(),
                   is(equalTo(PUBLISHED)));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, mode = Mode.EXCLUDE, names = {"DRAFT", "PUBLISHED_METADATA",
        "PUBLISHED", "DELETED", "UNPUBLISHED"})
    void shouldNotAllowPublish(PublicationStatus status) {
        var publication = randomPublication().copy().withStatus(status).build();
        resourceService.insertPreexistingPublication(publication);
        assertThrows(IllegalStateException.class, () -> Resource.fromPublication(publication)
                                                            .publish(resourceService,
                                                                     UserInstance.fromPublication(publication)));
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
        Resource.fromPublication(publication).publish(resourceService, userInstance);
        resourceService.unpublishPublication(resourceService.getPublicationByIdentifier(publication.getIdentifier()),
                                             userInstance);
        resourceService.terminateResource(resourceService.getResourceByIdentifier(publication.getIdentifier()),
                                          userInstance);

        var deletedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(deletedPublication.getStatus(), is(equalTo(PublicationStatus.DELETED)));
    }

    @Test
    void shouldUpdatePublicationVersionWhenRefreshingResource() throws ApiGatewayException {
        var publication = createPublishedResource();
        var version = Resource.fromPublication(publication).toDao().getVersion();
        resourceService.refreshResource(publication.getIdentifier());
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var updatesVersion = Resource.fromPublication(updatedPublication).toDao().getVersion();

        assertThat(updatesVersion, is(not(equalTo(version))));
    }

    @Test
    void shouldLogWhenPublicationToRefreshDoesNotExist() {
        var publication = randomPublication();
        var appender = LogUtils.getTestingAppender(ResourceService.class);
        resourceService.refreshResource(publication.getIdentifier());
        assertThat(appender.getMessages(), Matchers.containsString("Resource to refresh is not found"));
    }

    @Test
    void shouldThrowBadRequestWhenAttemptingToDeletePublishedPublication() throws ApiGatewayException {
        var publication = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(publication);
        Resource.fromPublication(publication).publish(resourceService, userInstance);

        assertThrows(BadRequestException.class, () -> resourceService.terminateResource(
            resourceService.getResourceByIdentifier(publication.getIdentifier()), userInstance));
    }

    @Test
    void shouldUpdatePublicationByImportedEntry() throws ApiGatewayException {
        var publication = createPublishedResource();
        publication.setDoi(randomDoi());
        var updatedPublication = resourceService.updatePublicationByImportEntry(publication, ImportSource.fromSource(
            Source.CRISTIN));

        assertThat(updatedPublication.getImportDetails().size(), is(equalTo(1)));
        assertThat(updatedPublication.getImportDetails().getFirst().importSource().getSource(),
                   is(equalTo(Source.CRISTIN)));
    }

    @Test
    void shouldImportResourceCreatedEventWhenCreatingNewPublication() throws ApiGatewayException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var resource = resourceService.getResourceByIdentifier(peristedPublication.getIdentifier());

        assertNotNull(resource.getResourceEvent());
    }

    @Test
    void shouldFetchResource() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var resource = Resource.resourceQueryObject(peristedPublication.getIdentifier()).fetch(resourceService);

        assertEquals(peristedPublication, resource.orElseThrow().toPublication());
    }

    @Test
    void shouldRepublishResourceAndSetResourceEvent() throws ApiGatewayException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        Resource.fromPublication(peristedPublication).publish(resourceService, userInstance);
        resourceService.unpublishPublication(peristedPublication, userInstance);
        Resource.resourceQueryObject(peristedPublication.getIdentifier())
            .fetch(resourceService)
            .orElseThrow()
            .republish(resourceService, ticketService, userInstance);

        var republishedResource = Resource.resourceQueryObject(peristedPublication.getIdentifier())
                                      .fetch(resourceService)
                                      .orElseThrow();

        assertEquals(PUBLISHED, republishedResource.getStatus());
        assertInstanceOf(RepublishedResourceEvent.class, republishedResource.getResourceEvent());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenRepublishingNotPublishedPublication() throws ApiGatewayException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        assertThrows(IllegalStateException.class,
                     () -> Resource.resourceQueryObject(peristedPublication.getIdentifier())
                               .fetch(resourceService)
                               .orElseThrow()
                               .republish(resourceService, ticketService, userInstance));
    }

    @Test
    void shouldNotPublishAlreadyPublishedPublication() throws ApiGatewayException {
        resourceService = mock(ResourceService.class);
        var publishedPublication = randomPublication().copy().withStatus(PUBLISHED).build();
        when(resourceService.getResourceByIdentifier(any())).thenReturn(Resource.fromPublication(publishedPublication));
        Resource.resourceQueryObject(publishedPublication.getIdentifier())
            .publish(resourceService, UserInstance.fromPublication(publishedPublication));

        verify(resourceService, never()).updateResource(any(), any());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenPublishingNotPublishableResource() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var peristedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var resource = Resource.fromPublication(peristedPublication).fetch(resourceService).orElseThrow();
        resource.setStatus(DRAFT_FOR_DELETION);
        resourceService.updateResource(resource, userInstance);

        assertThrows(IllegalStateException.class,
                     () -> Resource.resourceQueryObject(peristedPublication.getIdentifier())
                               .publish(resourceService, userInstance));
    }

    @Test
    void shouldSetAllTicketsToNotApplicableWhenMarkingDraftPublicationAsDraftForDeletion() throws ApiGatewayException {
        var publication = createPersistedPublicationWithoutDoi();
        var ticket = TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class)
                         .withOwnerAffiliation(randomUri())
                         .withOwner(randomString())
                         .persistNewTicket(ticketService);
        resourceService.markPublicationForDeletion(UserInstance.fromPublication(publication),
                                                   publication.getIdentifier());

        assertEquals(TicketStatus.NOT_APPLICABLE, ticket.fetch(ticketService).getStatus());
    }

    @Test
    void shouldThrowBadRequestExceptionWhenMarkingNotExistingPublicationAsDraftForDeletion() {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        Executable action = () -> resourceService.markPublicationForDeletion(userInstance, publication.getIdentifier());
        assertThrows(BadRequestException.class, action);
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
    void shouldHardDeleteFile() throws BadRequestException {
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
    void shouldSoftDeleteFile() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);
        fileEntry.softDelete(resourceService, new User(randomString()));

        assertInstanceOf(FileDeletedEvent.class, fileEntry.fetch(resourceService).orElseThrow().getFileEvent());
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
        fileEntry.update(updatedFile, userInstance, resourceService);

        assertEquals(updatedFile, fileEntry.fetch(resourceService).orElseThrow().getFile());
    }

    @Test
    void shouldUpdateFileEntryOwnerAffiliation() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(randomHiddenFile(), resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        var newOwnerAffiliation = randomUri();
        var originalModifiedDate = fileEntry.getModifiedDate();
        fileEntry.updateOwnerAffiliation(resourceService, newOwnerAffiliation);

        var updatedFileEntry = fileEntry.fetch(resourceService).orElseThrow();

        assertNotEquals(originalModifiedDate, updatedFileEntry.getModifiedDate());
        assertEquals(newOwnerAffiliation, updatedFileEntry.getOwnerAffiliation());
    }

    @Test
    void shouldFetchPublicationFromFile() throws BadRequestException, NotFoundException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        persistedPublication = resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());

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

        var resource = Resource.fromPublication(persistedPublication).fetch(resourceService);

        assertTrue(resource.orElseThrow().getAssociatedArtifacts().contains(file));
    }

    @Test
    void shouldRejectPersistedFileAndSetFileEventWithFileTypeThatHasBeenRejected() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingOpenFile();
        var resourceIdentifier = persistedPublication.getIdentifier();

        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        fileEntry.persist(resourceService);

        fileEntry.reject(resourceService, new User(randomString()));

        var rejectedFileEntry = fileEntry.fetch(resourceService).orElseThrow();

        var fileEvent = (FileRejectedEvent) rejectedFileEntry.getFileEvent();
        assertEquals(file.getArtifactType(), fileEvent.rejectedFileType());
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

        fileEntry.approve(resourceService, new User(randomString()));

        var rejectedFileEntry = fileEntry.fetch(resourceService).orElseThrow();

        assertInstanceOf(OpenFile.class, rejectedFileEntry.getFile());
    }

    @Test
    void shouldFetchResourceWithNewFilesWhenEnvironmentVariableShouldUseNewFileIsSet() throws BadRequestException {
        var resourceService = getResourceServiceBuilder(client).build();
        var publication = randomPublication().copy().withAssociatedArtifacts(new ArrayList<>()).build();
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
        var resourceService = getResourceServiceBuilder().build();

        var publication = randomPublication().copy().withAssociatedArtifacts(new ArrayList<>()).build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingInternalFile();
        FileEntry.create(file, publication.getIdentifier(), userInstance).persist(resourceService);

        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createWithFilesForApproval(
            Resource.fromPublication(publication), userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY,
            Set.of(file)).persistNewTicket(ticketService);

        publishingRequest.approveFiles().persistUpdate(ticketService);
        publishingRequest.setFinalizedBy(new Username(randomString()));
        publishingRequest.publishApprovedFiles(resourceService);

        assertInstanceOf(InternalFile.class, FileEntry.queryObject(file.getIdentifier(), publication.getIdentifier())
                                                 .fetch(resourceService)
                                                 .orElseThrow()
                                                 .getFile());
    }

    @Test
    void shouldApproveApprovedFilesWhenFilesAreInAssociatedArtifacts() throws ApiGatewayException {
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createWithFilesForApproval(
            Resource.fromPublication(publication), userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY,
            Set.of(file)).persistNewTicket(ticketService);
        publishingRequest.approveFiles().close(randomUserInstance()).persistUpdate(ticketService);
        publishingRequest = (PublishingRequestCase) publishingRequest.fetch(ticketService);

        publishingRequest.publishApprovedFiles(resourceService);

        var associatedArtifact = Resource.fromPublication(publication)
                                     .fetch(resourceService)
                                     .orElseThrow()
                                     .getAssociatedArtifacts()
                                     .getFirst();
        assertInstanceOf(InternalFile.class, associatedArtifact);
    }

    @Test
    void shouldRejectRejectedFilesWhenShouldUseNewFilesIsPresent() throws ApiGatewayException {
        var resourceService = getResourceServiceBuilder().build();

        var publication = randomPublication().copy().withAssociatedArtifacts(new ArrayList<>()).build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var file = randomPendingInternalFile();
        FileEntry.create(file, publication.getIdentifier(), userInstance).persist(resourceService);

        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createWithFilesForApproval(
            Resource.fromPublication(publication), UserInstance.create(randomString(), randomUri()),
            PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY, Set.of(file)).persistNewTicket(ticketService);

        publishingRequest.setFinalizedBy(new Username(randomString()));
        publishingRequest.rejectRejectedFiles(resourceService);

        assertInstanceOf(RejectedFile.class, FileEntry.queryObject(file.getIdentifier(), publication.getIdentifier())
                                                 .fetch(resourceService)
                                                 .orElseThrow()
                                                 .getFile());
    }

    @Test
    void shouldRejectRejectedFilesWhenFilesAreInAssociatedArtifacts() throws ApiGatewayException {
        var file = randomPendingInternalFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(file)).build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createWithFilesForApproval(
            Resource.fromPublication(publication), userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY,
            Set.of(file)).persistNewTicket(ticketService);
        publishingRequest.close(randomUserInstance()).persistUpdate(ticketService);
        publishingRequest = (PublishingRequestCase) publishingRequest.fetch(ticketService);
        publishingRequest.rejectRejectedFiles(resourceService);

        var associatedArtifact = Resource.fromPublication(publication)
                                     .fetch(resourceService)
                                     .orElseThrow()
                                     .getAssociatedArtifacts()
                                     .getFirst();

        assertInstanceOf(RejectedFile.class, associatedArtifact);
    }

    @Test
    void resourceShouldContainFileEntries() throws BadRequestException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of()).build();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var fileEntry = FileEntry.create(randomOpenFile(), resource.getIdentifier(), userInstance);
        fileEntry.persist(resourceService);

        var resourceWithFileEntry = Resource.resourceQueryObject(resource.getIdentifier())
                                        .fetch(resourceService)
                                        .orElseThrow();

        assertTrue(resourceWithFileEntry.getFileEntries().contains(fileEntry));
        assertNotNull(resourceWithFileEntry.getFileEntry(fileEntry.getIdentifier()));
    }

    @Test
    void shouldImportResourceAndSetImportedResourceEventWhenImportingPublication() {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .importResource(resourceService, ImportSource.fromSource(Source.SCOPUS));

        var resourceEvent = (ImportedResourceEvent) resource.getResourceEvent();

        assertEquals(Source.SCOPUS, resourceEvent.importSource().getSource());
    }

    @Test
    void shouldUpdateResourceFromImportAndSetMergedResourceEventWhenUpdatingExistingPublication()
        throws BadRequestException, NotFoundException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        resource.setDoi(randomDoi());
        Resource.fromPublication(resource)
            .updateResourceFromImport(resourceService, ImportSource.fromSource(Source.SCOPUS));
        var updatedResource = resourceService.getResourceByIdentifier(resource.getIdentifier());

        var resourceEvent = (MergedResourceEvent) updatedResource.getResourceEvent();

        assertEquals(Source.SCOPUS, resourceEvent.importSource().getSource());
    }

    @Test
    void shouldSetFileTypeRetractedEventWhenRetractingFinalizedFile() throws BadRequestException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of()).build();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var openFile = randomOpenFile();
        var fileEntry = FileEntry.create(openFile, resource.getIdentifier(), userInstance);
        fileEntry.persist(resourceService);

        var pendingFile = openFile.copy().buildPendingInternalFile();

        fileEntry.fetch(resourceService).orElseThrow().update(pendingFile, userInstance, resourceService);

        var updatedFileEntry = fileEntry.fetch(resourceService).orElseThrow();
        assertInstanceOf(FileRetractedEvent.class, updatedFileEntry.getFileEvent());
    }

    @Test
    void shouldSetFileTypeHiddenEventWhenUpdatingFileToHidden() throws BadRequestException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of()).build();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var openFile = randomOpenFile();
        var fileEntry = FileEntry.create(openFile, resource.getIdentifier(), userInstance);
        fileEntry.persist(resourceService);

        var hiddenFile = openFile.copy().buildHiddenFile();

        fileEntry.fetch(resourceService).orElseThrow().update(hiddenFile, userInstance, resourceService);

        var updatedFileEntry = fileEntry.fetch(resourceService).orElseThrow();
        assertInstanceOf(FileHiddenEvent.class, updatedFileEntry.getFileEvent());
    }

    @Test
    void shouldUpdateResource() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var doi = randomUri();
        persistedPublication.setDoi(doi);
        var updatedResource = Resource.fromPublication(persistedPublication).update(resourceService, userInstance);

        assertEquals(doi, updatedResource.getDoi());
    }

    @Test
    void shouldSetFileImportedEventWhenFileIsBeingImported() throws BadRequestException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of()).build();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var openFile = randomOpenFile();
        var fileEntry = FileEntry.create(openFile, resource.getIdentifier(), userInstance);
        fileEntry.importNew(resourceService, userInstance, ImportSource.fromBrageArchive("ntnu"));

        var updatedFileEntry = fileEntry.fetch(resourceService).orElseThrow();

        assertInstanceOf(FileImportedEvent.class, updatedFileEntry.getFileEvent());
    }

    @Test
    void updateResourceMethodShouldRefreshTickets() throws ApiGatewayException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of()).build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var ticket = createDoiRequest(publication);
        publication.setDoi(randomUri());
        Resource.fromPublication(publication).update(resourceService, userInstance);

        var refreshedTicket = ticket.fetch(ticketService);

        assertNotEquals(ticket.getModifiedDate(), refreshedTicket.getModifiedDate());
    }

    @Test
    void shouldUpdateFileVersionOnlyWhenRefreshing() throws BadRequestException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of()).build();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var openFile = randomOpenFile();
        var fileEntry = FileEntry.create(openFile, resource.getIdentifier(), userInstance);
        fileEntry.persist(resourceService);

        var persistedFileEntry = fileEntry.fetch(resourceService).orElseThrow();
        var persistedResult = client.getItem(new GetItemRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                                                 .withKey(fileEntry.toDao().primaryKey()));
        var persistedDao = Optional.ofNullable(persistedResult.getItem()).map(FileDao::fromDynamoFormat).orElseThrow();

        resourceService.refreshFile(fileEntry.getIdentifier());

        var refreshedFileEntry = fileEntry.fetch(resourceService).orElseThrow();
        var refreshedResult = client.getItem(new GetItemRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                                                 .withKey(fileEntry.toDao().primaryKey()));
        var refreshedDao = Optional.ofNullable(refreshedResult.getItem()).map(FileDao::fromDynamoFormat).orElseThrow();

        assertEquals(persistedFileEntry, refreshedFileEntry);
        assertNotEquals(persistedDao.getVersion(), refreshedDao.getVersion());
    }

    @Test
    void scanResourcesShouldScanFileEntries() throws BadRequestException {
        createPersistedPublicationWithDoi();
        var entries = resourceService.scanResources(BIG_PAGE, ListingResult.empty().getStartMarker(),
                                                    Collections.emptyList());

        assertTrue(entries.getDatabaseEntries().stream().anyMatch(FileEntry.class::isInstance));
    }

    @Test
    void scanResourcesShouldNotFailScanningLogEntries() {
        var userInstance = UserInstance.create(randomString(), randomUri());
        resourceService.persistLogEntry(CreatedResourceEvent.create(userInstance, Instant.now())
                                            .toLogEntry(SortableIdentifier.next(),
                                                        new LogOrganization(randomUri(), randomString(), Map.of())));
        assertDoesNotThrow(() -> resourceService.scanResources(BIG_PAGE, ListingResult.empty().getStartMarker(),
                                                               Collections.emptyList()));
    }

    @Test
    void shouldNotUpdatePublicationInDatabaseWhenNoEffectiveChange() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var updatedResource = Resource.fromPublication(publication).update(resourceService, userInstance);

        assertEquals(publication, updatedResource.toPublication());
    }

    @Test
    void shouldUpdatePublicationInDatabaseWhenThereAreEffectiveChange() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        publication.setDoi(randomUri());
        var updatedResource = Resource.fromPublication(publication).update(resourceService, userInstance);

        assertNotEquals(publication, updatedResource.toPublication());
    }

    @Test
    void shouldFetchPublicationForFileApprovalThesis() throws BadRequestException {
        var publication = randomPublication(DegreeBachelor.class);
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var fileApprovalThesis = FilesApprovalThesis.createForUserInstitution(Resource.fromPublication(publication),
                                                                              userInstance,
                                                                              PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);

        var fetchedPublication = fileApprovalThesis.toPublication(resourceService);

        assertEquals(publication, fetchedPublication);
    }

    @Test
    void shouldUpdateOnlyVersionWhenRefreshingResource() throws BadRequestException {
        var resource = Resource.fromPublication(randomPublication());
        var publication = resource.persistNew(resourceService, randomUserInstance());
        var persistedResource = Resource.fromPublication(publication).fetch(resourceService).orElseThrow();
        var persistedDao = getDao(persistedResource);

        resourceService.refreshResource(publication.getIdentifier());

        var refreshedResource = Resource.fromPublication(publication).fetch(resourceService).orElseThrow();
        var refreshedDao = getDao(persistedResource);

        assertEquals(persistedResource, refreshedResource);
        assertNotEquals(persistedDao.getVersion(), refreshedDao.getVersion());
    }

    private void createTickets(Resource resource, UserInstance userInstance) throws ApiGatewayException {
        GeneralSupportRequest.create(resource, userInstance).persistNewTicket(ticketService);
        DoiRequest.create(resource, userInstance).persistNewTicket(ticketService);
        var closedGeneralSupportTicket = GeneralSupportRequest.create(resource, userInstance)
                                             .persistNewTicket(ticketService)
                                             .close(randomUserInstance());
        ticketService.updateTicket(closedGeneralSupportTicket);
        var closedPublishingRequestTicket = PublishingRequestCase.create(resource, userInstance,
                                                                         PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        closedPublishingRequestTicket.setStatus(TicketStatus.COMPLETED);
        closedPublishingRequestTicket.persistNewTicket(ticketService);

        var pendingPublishingRequestTicket = PublishingRequestCase.create(resource, userInstance,
                                                                          PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        pendingPublishingRequestTicket.setStatus(TicketStatus.PENDING);
        pendingPublishingRequestTicket.persistNewTicket(ticketService);
    }

    private static UserInstance randomUserInstance() {
        return UserInstance.create(randomString(), randomUri());
    }

    private static AssociatedArtifactList createEmptyArtifactList() {
        return new AssociatedArtifactList(emptyList());
    }

    private Dao getDao(Resource persistedResource) {
        var getRefreshedResourceResult = client.getItem(
            new GetItemRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                .withKey(persistedResource.toDao().primaryKey()));
        return parseAttributeValuesMap(getRefreshedResourceResult.getItem(), Dao.class);
    }

    private ResourceService getResourceServiceWithDuplicateIdentifier(SortableIdentifier identifier) {
        return getResourceServiceBuilder(client).withIdentifierSupplier(() -> identifier)
                   .withUriRetriever(mock(UriRetriever.class))
                   .build();
    }

    private Username randomPerson() {
        return new Username(randomString());
    }

    private void persistSamplePublicationsOfSingleCristinIdentifier(String cristinIdentifier) {
        UserInstance userInstance = randomUserInstance();
        Stream.of(publicationWithIdentifier(), publicationWithIdentifier(), publicationWithIdentifier())
            .map(publication -> injectOwner(userInstance, publication))
            .map(publication -> injectCristinIdentifier(cristinIdentifier, publication))
            .forEach(res -> attempt(() -> createPersistedPublicationWithDoi(resourceService, res)));
    }

    private Publication injectCristinIdentifier(String cristinIdentifier, Publication publication) {
        publication.setAdditionalIdentifiers(
            Set.of(new CristinIdentifier(SourceName.fromCristin("ntnu@123"), cristinIdentifier)));
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
        return new ImportCandidate.Builder().withStatus(PublicationStatus.PUBLISHED)
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
                   .withAssociatedArtifacts(List.of(randomOpenFile()))
                   .withEntityDescription(randomEntityDescription(JournalArticle.class))
                   .build();
    }

    private Publication createPersistedPublicationWithManyContributions(int amount) throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();
        var contributions = IntStream.rangeClosed(1, amount).mapToObj(i -> randomContributor()).toList();
        publication.getEntityDescription().setContributors(contributions);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication createPersistedPublicationWithManyContributionsWithoutAffiliations(int amount)
        throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();
        var contributions = IntStream.rangeClosed(1, amount).mapToObj(i -> randomContributor(List.of())).toList();
        publication.getEntityDescription().setContributors(contributions);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Contributor randomContributor() {
        return randomContributor(List.of(randomOrganization()));
    }

    private Contributor randomContributor(List<Corporation> affiliations) {
        return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withSequence(randomInteger(10000))
                   .withAffiliations(affiliations)
                   .withIdentity(randomIdentity())
                   .build();
    }

    private Identity randomIdentity() {
        return new Identity.Builder().withName(randomString())
                   .withOrcId(randomString())
                   .withId(RandomPersonServiceResponse.randomUri())
                   .build();
    }

    private Publication draftPublicationWithoutDoiAndAssociatedLink() {

        return randomPublication().copy()
                   .withDoi(null)
                   .withStatus(DRAFT)
                   .withAssociatedArtifacts(List.of(randomAssociatedLink()))
                   .build();
    }

    private Publication createPersistedPublicationWithoutDoi() throws BadRequestException {
        var publication = randomPublication(JournalArticle.class).copy().withDoi(null).build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication createPersistedPublicationWithoutDoi(Publication publication) throws BadRequestException {
        var withoutDoi = publication.copy().withDoi(null).build();
        return Resource.fromPublication(withoutDoi)
                   .persistNew(resourceService, UserInstance.fromPublication(withoutDoi));
    }

    private Publication createPersistedPublicationWithDoi(ResourceService resourceService, Publication sampleResource)
        throws BadRequestException {
        return Resource.fromPublication(sampleResource)
                   .persistNew(resourceService, UserInstance.fromPublication(sampleResource));
    }

    private Publication createPersistedPublicationWithDoi() throws BadRequestException {
        return createPersistedPublicationWithDoi(resourceService, randomPublication(JournalArticle.class));
    }

    private Publication createUnpublishablePublication() throws BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
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

    private Publication publishResource(Publication publication) throws ApiGatewayException {
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        return resourceService.getPublicationByIdentifier(publication.getIdentifier());
    }

    private void assertThatIdentifierEntryHasBeenCreated() {
        assertThatResourceAndIdentifierEntryExist();
    }

    private void assertThatResourceAndIdentifierEntryExist() {
        ScanResult result = client.scan(new ScanRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME));
        assertThat(result.getCount(), is(doesNotHaveEmptyValues()));
    }

    private void assertThatTheEntriesHaveNotBeenDeleted() {
        assertThatResourceAndIdentifierEntryExist();
    }

    private void assertThatAllEntriesHaveBeenDeleted() {
        ScanResult result = client.scan(new ScanRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME));
        assertThat(result.getCount(), is(equalTo(0)));
    }

    private DoiRequest createDoiRequest(Publication publication) throws ApiGatewayException {
        return (DoiRequest) DoiRequest.create(Resource.fromPublication(publication),
                                              UserInstance.fromPublication(publication))
                                .persistNewTicket(ticketService);
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
        return resourceService.getPublicationByIdentifier(resource.getIdentifier());
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
                   .withResourceOwner(
                       new ResourceOwner(new Username(userInstance.getUsername()), AFFILIATION_NOT_IMPORTANT))
                   .withPublisher(new Organization.Builder().withId(userInstance.getCustomerId()).build())
                   .build();
    }

    private Publication expectedUpdatedResource(Publication originalResource, Publication updatedResource,
                                                UserInstance expectedOwner) {
        return originalResource.copy()
                   .withResourceOwner(
                       new ResourceOwner(new Username(expectedOwner.getUsername()), AFFILIATION_NOT_IMPORTANT))
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

    private void assertThatResourceHaveNewOwner(Publication sampleResource) {
        try {
            assertThat(resourceService.getPublicationByIdentifier(sampleResource.getIdentifier())
                           .getResourceOwner()
                           .getOwner(), is(not(equalTo(sampleResource.getResourceOwner().getOwner()))));
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
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