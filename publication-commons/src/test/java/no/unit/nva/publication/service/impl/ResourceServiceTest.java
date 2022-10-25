package no.unit.nva.publication.service.impl;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static java.util.Collections.emptyList;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ResourceService.ASSOCIATED_ARIFACTS_FIELD;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.publication.service.impl.UpdateResourceService.RESOURCE_LINK_FIELD;
import static no.unit.nva.publication.service.impl.UpdateResourceService.RESOURCE_WITHOUT_MAIN_TITLE_ERROR;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.HttpURLConnection;
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
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.License;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.testutils.RandomDataGenerator;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ResourceServiceTest extends ResourcesLocalTest {
    
    public static final String ANOTHER_OWNER = "another@owner.no";
    public static final String SOME_OTHER_USER = "some_other@user.no";
    
    public static final String UPDATED_TITLE = "UpdatedTitle";
    public static final String SOME_INVALID_FIELD = "someInvalidField";
    public static final String SOME_STRING = "someValue";
    public static final String MAIN_TITLE_FIELD = "mainTitle";
    public static final String ENTITY_DESCRIPTION_DOES_NOT_HAVE_FIELD_ERROR = EntityDescription.class.getName()
                                                                              + " does not have a field"
                                                                              + MAIN_TITLE_FIELD;
    public static final String ANOTHER_TITLE = "anotherTitle";
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final int BIG_PAGE = 10;
    public static final URI UNIMPORTANT_AFFILIATION = null;
    public static final URI AFFILIATION_NOT_IMPORTANT = null;
    private static final URI SOME_ORG = randomUri();
    public static final UserInstance SAMPLE_USER = UserInstance.create(randomString(), SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
    private static final URI SOME_LINK = URI.create("http://www.example.com/someLink");
    private static final boolean NOT_ADMINISTRATIVE_AGREEMENT = false;
    private ResourceService resourceService;
    
    private TicketService ticketService;
    private MessageService messageService;
    private Instant now;
    private Clock clock;
    
    @BeforeEach
    public void init() {
        super.init();
        clock = Clock.systemDefaultZone();
        now = clock.instant();
        resourceService = new ResourceService(client, clock);
        ticketService = new TicketService(client);
        messageService = new MessageService(client);
    }
    
    public Optional<ResourceDao> searchForResource(ResourceDao resourceDaoWithStatusDraft) {
        QueryResult queryResult = queryForDraftResource(resourceDaoWithStatusDraft);
        return parseResult(queryResult);
    }
    
    @Test
    void shouldKeepImportedDataCreationDates()
        throws NotFoundException {
        var randomInstant = RandomDataGenerator.randomInstant();
        var inputPublication = randomPublication().copy().withCreatedDate(randomInstant).build();
        
        var savedPublicationIdentifier =
            resourceService.createPublicationWithPredefinedCreationDate(inputPublication).getIdentifier();
        var savedPublication = resourceService.getPublicationByIdentifier(savedPublicationIdentifier);
        
        // inject publicationIdentifier for making the inputPublication and the savedPublication equal.
        inputPublication.setIdentifier(savedPublicationIdentifier);
        
        var possiblyErrorDiff = JAVERS.compare(inputPublication, savedPublication);
        assertThat(possiblyErrorDiff.prettyPrint(), savedPublication, is(equalTo(inputPublication)));
    }
    
    @Test
    void shouldKeepImportedEntryCreationAndModifiedDates()
        throws NotFoundException {
        var createdDate = randomInstant();
        var modifiedDate = randomInstant();
        var inputPublication = randomPublication().copy()
                                   .withCreatedDate(createdDate)
                                   .withModifiedDate(modifiedDate)
                                   .withStatus(PUBLISHED)
                                   .build();
        var savedPublicationIdentifier =
            resourceService.createPublicationFromImportedEntry(inputPublication).getIdentifier();
        var savedPublication = resourceService.getPublicationByIdentifier(savedPublicationIdentifier);
        
        // inject publicationIdentifier for making the inputPublication and the savedPublication equal.
        inputPublication.setIdentifier(savedPublicationIdentifier);
        
        assertThat(savedPublication, is(equalTo(inputPublication)));
        assertThat(savedPublication.getStatus(), is(equalTo(PUBLISHED)));
    }
    
    @Test
    void createResourceReturnsResourceWithCreatedAndModifiedDateSetByThePlatform() throws ApiGatewayException {
    
        var input = generatePublication();
        var notExpectedCreatedDate = randomInstant();
        var notExpectedModifiedDate = randomInstant();
        input.setCreatedDate(notExpectedCreatedDate);
        input.setModifiedDate(notExpectedModifiedDate);
    
        var userInstance = UserInstance.fromPublication(input);
        var savedResource = resourceService.createPublication(userInstance, input);
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
    void createResourceThrowsTransactionFailedExceptionWhenResourceWithSameIdentifierExists() {
        final Publication sampleResource = randomPublication();
        final Publication collidingResource = sampleResource.copy()
                                                  .withPublisher(anotherPublisher())
                                                  .withResourceOwner(new ResourceOwner(SOME_OTHER_USER, null))
                                                  .build();
        ResourceService resourceService = resourceServiceProvidingDuplicateIdentifiers(sampleResource.getIdentifier());
        
        createPersistedPublicationWithDoi(resourceService, sampleResource);
        Executable action = () -> createPersistedPublicationWithDoi(resourceService, collidingResource);
        assertThrows(TransactionFailedException.class, action);
        
        assertThat(sampleResource.getIdentifier(), is(equalTo(collidingResource.getIdentifier())));
        assertThat(sampleResource.getResourceOwner().getOwner(),
            is(not(equalTo(collidingResource.getResourceOwner().getOwner()))));
        assertThat(sampleResource.getPublisher().getId(), is(not(equalTo(collidingResource.getPublisher().getId()))));
    }
    
    @Test
    void createResourceSavesResourcesWithSameOwnerAndPublisherButDifferentIdentifier() {
        final Publication sampleResource = publicationWithIdentifier();
        final Publication anotherResource = publicationWithIdentifier();
        
        createPersistedPublicationWithDoi(resourceService, sampleResource);
        assertDoesNotThrow(() -> createPersistedPublicationWithDoi(resourceService, anotherResource));
    }
    
    @Test
    void createPublicationReturnsNullWhenResourceDoesNotBecomeAvailable() {
        Publication publication = publicationWithoutIdentifier();
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        
        ResourceService resourceService = resourceServiceThatDoesNotReceivePublicationUpdateAfterCreation(client);
        Publication actualPublication = createPersistedPublicationWithDoi(resourceService, publication);
        assertThat(actualPublication, is(nullValue()));
    }
    
    @Test
    void getResourceByIdentifierReturnsNotFoundWhenResourceDoesNotExist() {
        SortableIdentifier nonExistingIdentifier = SortableIdentifier.next();
        Executable action = () -> resourceService.getPublication(SAMPLE_USER, nonExistingIdentifier);
        assertThrows(NotFoundException.class, action);
    }
    
    @Test
    void getResourceByIdentifierReturnsResourceWhenResourceExists()
        throws ApiGatewayException {
        Publication sampleResource = createPersistedPublicationWithDoi();
        UserInstance userInstance = UserInstance.fromPublication(sampleResource);
        Publication savedResource = resourceService.getPublication(userInstance, sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }
    
    @Test
    void whenPublicationOwnerIsUpdatedTheResourceEntryMaintainsTheRestResourceMetadata()
        throws ApiGatewayException {
        var originalResource = createPersistedPublicationWithDoi();
    
        var oldOwner = UserInstance.fromPublication(originalResource);
        var newOwner = someOtherUser();
    
        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);
    
        assertThatResourceDoesNotExist(originalResource);
    
        var newResource = resourceService.getPublication(newOwner, originalResource.getIdentifier());
    
        var expectedResource = expectedUpdatedResource(originalResource, newResource, newOwner);
        var diff = JAVERS.compare(expectedResource, newResource);
        assertThat(diff.prettyPrint(), newResource, is(equalTo(expectedResource)));
    }
    
    @Test
    void whenPublicationOwnerIsUpdatedThenBothOrganizationAndUserAreUpdated()
        throws ApiGatewayException {
        Publication originalResource = createPersistedPublicationWithDoi();
        UserInstance oldOwner = UserInstance.fromPublication(originalResource);
        UserInstance newOwner = someOtherUser();
        
        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);
        
        Publication newResource = resourceService.getPublication(newOwner, originalResource.getIdentifier());
    
        assertThat(newResource.getResourceOwner().getOwner(), is(equalTo(newOwner.getUsername())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getOrganizationUri())));
    }
    
    @Test
    void whenPublicationOwnerIsUpdatedTheModifiedDateIsUpdated()
        throws ApiGatewayException {
        Publication sampleResource = createPersistedPublicationWithDoi();
        UserInstance oldOwner = UserInstance.fromPublication(sampleResource);
        UserInstance newOwner = someOtherUser();
        
        resourceService.updateOwner(sampleResource.getIdentifier(), oldOwner, newOwner);
        
        assertThatResourceDoesNotExist(sampleResource);
        
        Publication newResource = resourceService.getPublication(newOwner, sampleResource.getIdentifier());
    
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
    void resourceUpdateFailsWhenUpdateChangesTheOwnerPartOfThePrimaryKey() {
        Publication resource = createPersistedPublicationWithDoi();
        Publication resourceUpdate = updateResourceTitle(resource);
        resourceUpdate.setResourceOwner(new ResourceOwner(ANOTHER_OWNER, UNIMPORTANT_AFFILIATION));
        assertThatUpdateFails(resourceUpdate);
    }
    
    @Test
    void resourceUpdateFailsWhenUpdateChangesTheOrganizationPartOfThePrimaryKey() {
        Publication resource = createPersistedPublicationWithDoi();
        Publication resourceUpdate = updateResourceTitle(resource);
        
        resourceUpdate.setPublisher(newOrganization());
        assertThatUpdateFails(resourceUpdate);
    }
    
    @Test
    void resourceUpdateFailsWhenUpdateChangesTheIdentifierPartOfThePrimaryKey() {
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
        when(client.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(expectedCause);
        
        ResourceService failingService = new ResourceService(client, clock);
    
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
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        String expectedMessage = "expectedMessage";
        RuntimeException exptedMessage = new RuntimeException(expectedMessage);
        when(client.getItem(any(GetItemRequest.class)))
            .thenThrow(exptedMessage);
        Publication resource = publicationWithIdentifier();
        
        ResourceService failingResourceService = new ResourceService(client, clock);
        
        Executable action = () -> failingResourceService.getPublication(resource);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), is(equalTo(expectedMessage)));
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
        
        ResourceService failingResourceService = new ResourceService(client, clock);
        
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> failingResourceService.getPublicationsByOwner(SAMPLE_USER));
        
        assertThat(exception.getMessage(), is(equalTo(expectedMessage)));
    }
    
    @Test
    void getResourcesByOwnerPropagatesJsonProcessingExceptionWhenExceptionIsThrown() {
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        Item invalidItem = new Item().withString(SOME_INVALID_FIELD, SOME_STRING);
        QueryResult responseWithInvalidItem = new QueryResult()
                                                  .withItems(List.of(ItemUtils.toAttributeValues(invalidItem)));
        when(mockClient.query(any(QueryRequest.class))).thenReturn(responseWithInvalidItem);
        
        ResourceService failingResourceService = new ResourceService(mockClient, clock);
        Class<JsonProcessingException> expectedExceptionClass = JsonProcessingException.class;
        
        assertThatJsonProcessingErrorIsPropagatedUp(expectedExceptionClass,
            () -> failingResourceService.getPublicationsByOwner(SAMPLE_USER));
    }
    
    @Test
    void getResourcePropagatesJsonProcessingExceptionWhenExceptionIsThrown() {
        
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        Item invalidItem = new Item().withString(SOME_INVALID_FIELD, SOME_STRING);
        GetItemResult responseWithInvalidItem = new GetItemResult().withItem(ItemUtils.toAttributeValues(invalidItem));
        when(mockClient.getItem(any(GetItemRequest.class))).thenReturn(responseWithInvalidItem);
        
        ResourceService failingResourceService = new ResourceService(mockClient, clock);
        Class<JsonProcessingException> expectedExceptionClass = JsonProcessingException.class;
        
        SortableIdentifier someIdentifier = SortableIdentifier.next();
        Executable action = () -> failingResourceService.getPublication(SAMPLE_USER, someIdentifier);
        
        assertThatJsonProcessingErrorIsPropagatedUp(expectedExceptionClass, action);
    }
    
    @Test
    void shouldPublishResourceWhenClientRequestsToPublish()
        throws ApiGatewayException {
        var resource = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(resource);
        resourceService.publishPublication(userInstance, resource.getIdentifier());
        var actualResource = resourceService.getPublication(resource);
    
        var expectedResource = resource.copy()
                                   .withStatus(PUBLISHED)
                                   .withModifiedDate(actualResource.getModifiedDate())
                                   .withPublishedDate(actualResource.getPublishedDate())
                                   .build();
        
        assertThat(actualResource, is(equalTo(expectedResource)));
    }
    
    @Test
    void publishPublicationReturnsResponseThatRequestWasAcceptedWhenResourceIsNotPublished()
        throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        
        UserInstance userInstance = UserInstance.fromPublication(resource);
        PublishPublicationStatusResponse response = resourceService.publishPublication(
            userInstance, resource.getIdentifier());
        
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }
    
    @Test
    void publishPublicationReturnsPublicationResponseThatNoActionWasTakenWhenResourceIsAlreadyPublished()
        throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        
        UserInstance userInstance = UserInstance.fromPublication(resource);
        resourceService.publishPublication(userInstance, resource.getIdentifier());
        PublishPublicationStatusResponse response = resourceService.publishPublication(
            userInstance, resource.getIdentifier());
        
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NO_CONTENT)));
    }
    
    @Test
    void byTypeCustomerStatusIndexIsUpdatedWhenResourceIsUpdated() throws ApiGatewayException {
        Publication resourceWithStatusDraft = createPersistedPublicationWithDoi();
        ResourceDao resourceDaoWithStatusDraft = new ResourceDao(Resource.fromPublication(resourceWithStatusDraft));
        
        assertThatResourceCanBeFoundInDraftResources(resourceDaoWithStatusDraft);
        
        resourceService.publishPublication(UserInstance.fromPublication(resourceWithStatusDraft),
            resourceWithStatusDraft.getIdentifier());
        
        verifyThatTheResourceWasMovedFromtheDrafts(resourceDaoWithStatusDraft);
        
        verifyThatTheResourceIsInThePublishedResources(resourceWithStatusDraft);
    }
    
    @Test
    void publishPublicationSetsPublishedDateToBeCurrentDate() throws ApiGatewayException {
        Publication updatedResource = createPublishedResource();
        assertThat(updatedResource.getPublishedDate(), is(greaterThan(now)));
    }
    
    @Test
    void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoTitle() {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.getEntityDescription().setMainTitle(null);
        Publication savedResource = createPersistedPublicationWithoutDoi(sampleResource);
        
        Executable action = () -> resourceService.publishPublication(UserInstance.fromPublication(sampleResource),
            savedResource.getIdentifier());
        
        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, containsString(RESOURCE_WITHOUT_MAIN_TITLE_ERROR));
    }
    
    @Test
    void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoLinkAndNoFiles()
        throws NoSuchFieldException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(null);
        sampleResource.setAssociatedArtifacts(createEmptyArtifactList());
        Publication savedResource = createPersistedPublicationWithoutDoi(sampleResource);
        
        Executable action =
            () -> publishResource(savedResource);
        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();
        
        assertThat(actualMessage, containsString(InvalidPublicationException.ERROR_MESSAGE_TEMPLATE));
        assertThat(actualMessage, containsString(sampleResource.getClass()
                                                     .getDeclaredField(RESOURCE_LINK_FIELD).getName()));
        assertThat(actualMessage, containsString(sampleResource.getClass()
                                                     .getDeclaredField(ASSOCIATED_ARIFACTS_FIELD).getName()));
    }
    
    private static AssociatedArtifactList createEmptyArtifactList() {
        return new AssociatedArtifactList(emptyList());
    }
    
    @Test
    void publishResourcePublishesResourceWhenLinkIsPresentButNoFiles() throws ApiGatewayException {
        
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(SOME_LINK);
        sampleResource.setAssociatedArtifacts(createEmptyArtifactList());
        Publication savedResource = createPersistedPublicationWithoutDoi();
        Publication updatedResource =
            publishResource(savedResource);
        assertThat(updatedResource.getStatus().toString(), is(equalTo(PUBLISHED.toString())));
    }
    
    @Test
    void publishResourcePublishesResourceWhenResourceHasFilesButNoLink() throws ApiGatewayException {
    
        Publication sampleResource = createPersistedPublicationWithoutDoi();
        sampleResource.setLink(null);
    
        Publication updatedResource =
            publishResource(sampleResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PUBLISHED)));
    }
    
    @Test
    void shouldKeepTheResourceInSyncWithTheAssociatedDoiRequestWhenResourceIsPublished()
        throws ApiGatewayException {
        var publication = createPersistedPublicationWithoutDoi();
        
        var doiRequest = ticketService.createTicket(DoiRequest.fromPublication(publication), DoiRequest.class);
        assertThat(doiRequest.getResourceStatus(), is(equalTo(PublicationStatus.DRAFT)));
        
        var publishedResource = publishResource(publication);
        assertThat(publishedResource.getStatus(), is(equalTo(PUBLISHED)));
        
        var actualDoiRequest = (DoiRequest) ticketService.fetchTicket(doiRequest);
        assertThat(actualDoiRequest.getResourceStatus(), is(equalTo(PUBLISHED)));
    }
    
    @Test
    void markPublicationForDeletionThrowsExceptionWhenDeletingPublishedPublication() throws ApiGatewayException {
        Publication resource = createPublishedResource();
        Executable action =
            () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                resource.getIdentifier());
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE));
        assertThat(exception.getMessage(), containsString(resource.getIdentifier().toString()));
    }
    
    @Test
    void markPublicationForDeletionLogsConditionExceptionWhenUpdateConditionFails() throws ApiGatewayException {
        TestAppender testAppender = LogUtils.getTestingAppender(ResourceService.class);
        Publication resource = createPublishedResource();
        Executable action =
            () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                resource.getIdentifier());
        assertThrows(BadRequestException.class, action);
        assertThat(testAppender.getMessages(), containsString(ConditionalCheckFailedException.class.getName()));
    }
    
    @Test
    void createResourceReturnsNewIdentifierWhenResourceIsCreated() {
        Publication sampleResource = randomPublication();
        Publication savedResource = createPersistedPublicationWithDoi(resourceService, sampleResource);
        assertThat(savedResource.getIdentifier(), is(not(equalTo(sampleResource.getIdentifier()))));
    }
    
    @Test
    void deletePublicationCanMarkDraftForDeletion() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        
        Publication resourceUpdate =
            resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                resource.getIdentifier());
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
        
        Publication resourceForDeletion = resourceService.getPublication(resource);
        assertThat(resourceForDeletion.getStatus(),
            Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }
    
    @Test
    void markPublicationForDeletionReturnsUpdatedResourceWithStatusDraftForDeletion()
        throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithDoi();
        var userInstance = UserInstance.fromPublication(resource);
        var resourceUpdate =
            resourceService.markPublicationForDeletion(userInstance, resource.getIdentifier());
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
    void updateResourceUpdatesLinkedDoiRequestUponUpdate()
        throws ApiGatewayException {
        var resource = createPersistedPublicationWithoutDoi();
        final var expectedDoi = randomDoi();
        final var originalDoiRequest = createDoiRequest(resource);
    
        resource.getEntityDescription().setMainTitle(ANOTHER_TITLE);
        resource.setDoi(expectedDoi);
        var updatedPublication = resourceService.updatePublication(resource);
    
        var updatedDoiRequest = ticketService.fetchTicket(originalDoiRequest);
    
        var expectedDoiRequest = originalDoiRequest.copy();
        expectedDoiRequest.setPublicationDetails(
            expectedDoiRequest.getPublicationDetails().update(Resource.fromPublication(updatedPublication)));
        var diff = JAVERS.compare(updatedDoiRequest, expectedDoiRequest);
        assertThat(diff.prettyPrint(), updatedDoiRequest, is(equalTo(expectedDoiRequest)));
    
        assertThat(updatedDoiRequest, doesNotHaveEmptyValues());
    }
    
    @Test
    void updateResourceUpdatesAllFieldsInDoiRequest()
        throws ApiGatewayException {
        var initialPublication = createPersistedPublicationWithoutDoi();
        var initialDoiRequest = createDoiRequest(initialPublication);
        var publicationUpdate = updateAllPublicationFieldsExpectIdentifierStatusAndOwnerInfo(initialPublication);
        resourceService.updatePublication(publicationUpdate);
    
        var updatedDoiRequest = (DoiRequest) ticketService.fetchTicket(initialDoiRequest);
    
        var expectedDoiRequest = expectedDoiRequestAfterPublicationUpdate(
            initialPublication,
            initialDoiRequest,
            publicationUpdate,
            updatedDoiRequest
        );
    
        assertThat(updatedDoiRequest, doesNotHaveEmptyValues());
        assertThat(expectedDoiRequest, doesNotHaveEmptyValues());
        Diff diff = JAVERS.compare(updatedDoiRequest, expectedDoiRequest);
        assertThat(diff.prettyPrint(), updatedDoiRequest, is(equalTo(expectedDoiRequest)));
    }
    
    @Test
    void updateResourceDoesNotCreateDoiRequestWhenItDoesNotPreexist() {
        Publication resource = createPersistedPublicationWithoutDoi();
        resource.getEntityDescription().setMainTitle(ANOTHER_TITLE);
        resourceService.updatePublication(resource);
        
        var expectedNonExistingTicket =
            ticketService.fetchTicketByResourceIdentifier(resource.getPublisher().getId(),
                resource.getIdentifier(), DoiRequest.class);
        
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
    void deleteDraftPublicationThrowsExceptionWhenResourceHasDoi() {
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
    void deleteDraftPublicationThrowsExceptionWhenResourceIsPublished()
        throws ApiGatewayException {
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
    void deleteDraftPublicationDeletesDoiRequestWhenPublicationHasDoiRequest()
        throws ApiGatewayException {
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
    
    @Test
    void shouldScanEntriesInDatabaseAfterSpecifiedMarker() throws ApiGatewayException {
        var samplePublication = createPersistedPublicationWithoutDoi();
        var sampleTicket =
            TicketEntry.requestNewTicket(samplePublication, DoiRequest.class).persistNewTicket(ticketService);
    
        var userInstance = UserInstance.fromPublication(samplePublication);
    
        var sampleMessage = messageService.createMessage(sampleTicket, userInstance, randomString());
    
        var firstListingResult = fetchFirstDataEntry();
        var identifierInFirstScan = extractIdentifierFromFirstScanResult(firstListingResult);
    
        var secondListingResult = fetchRestOfDatabaseEntries(firstListingResult);
        var identifiersFromSecondScan = secondListingResult
                                            .getDatabaseEntries().stream()
                                            .map(Entity::getIdentifier)
                                            .collect(Collectors.toList());
        
        var expectedIdentifiers =
            new ArrayList<>(List.of(samplePublication.getIdentifier(),
                sampleTicket.getIdentifier(),
                sampleMessage.getIdentifier())
            );
        expectedIdentifiers.remove(identifierInFirstScan);
        assertThat(identifiersFromSecondScan,
            containsInAnyOrder(expectedIdentifiers.toArray(SortableIdentifier[]::new)));
    }
    
    @Test
    void shouldLogUserInformationQueryObjectAndResourceIdentifierWhenFailingToPublishResource() {
        
        var samplePublication = createUnpublishablePublication();
        var userInstance = UserInstance.fromPublication(samplePublication);
        var exception = assertThrows(InvalidPublicationException.class,
            () -> resourceService.publishPublication(userInstance, samplePublication.getIdentifier()));
        assertThat(exception.getMessage(), containsString(RESOURCE_WITHOUT_MAIN_TITLE_ERROR));
    }
    
    @Test
    void shouldSaveDraftPublicationWithAssociatedFilesAsUnpublished() {
        var publication = draftPublicationWithoutDoiAndAllTypesOfFiles();
        var persisted = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        assertThatAllFilesAreUnpublished(persisted);
    }
    
    private static void assertThatAllFilesAreUnpublished(Publication persisted) {
        for (var artifact : persisted.getAssociatedArtifacts()) {
            assertThat(artifact, is(instanceOf(UnpublishedFile.class)));
        }
    }
    
    private Publication draftPublicationWithoutDoiAndAllTypesOfFiles() {
        var legacyFile = randomFile().buildLegacyFile();
        var publishedFile = randomFile().buildPublishedFile();
        var unpublishedFile = randomFile().buildUnpublishedFile();
        return draftPublicationWithoutDoi()
                   .withAssociatedArtifacts(List.of(legacyFile, publishedFile, unpublishedFile))
                   .build();
    }
    
    @Test
    void shouldSaveAlreadyPublishedPublicationWithAssociatedFilesAsPublished() throws NotFoundException {
        var legacyFile = randomFile().buildLegacyFile();
        var publishedFile = randomFile().buildPublishedFile();
        var unpublishedFile = randomFile().buildUnpublishedFile();
        var publication = randomPublication()
                              .copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(List.of(legacyFile, publishedFile, unpublishedFile))
                              .build();
        var persisted = resourceService.createPublicationFromImportedEntry(publication);
        
        assertThatAllAssosicatedArtifactsArePublished(persisted);
    }
    
    private void assertThatAllAssosicatedArtifactsArePublished(Publication publication) throws NotFoundException {
        var fetched = resourceService.getPublication(publication);
        for (var artifact : fetched.getAssociatedArtifacts()) {
            assertThat(artifact, is(instanceOf(PublishedFile.class)));
        }
    }
    
    @Test
    void shouldConvertUnPublishedArtifactsToPublishedWhenPublicationIsPublished() throws ApiGatewayException {
        var publication = draftPublicationWithoutDoiAndAllTypesOfFiles();
        var persistedDraft = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        assertThatAllFilesAreUnpublished(persistedDraft);
        resourceService.publishPublication(UserInstance.fromPublication(persistedDraft),
            persistedDraft.getIdentifier());
        var persistedPublished = resourceService.getPublication(persistedDraft);
        assertThat(persistedPublished.getStatus(), is(equalTo(PUBLISHED)));
        assertThatAllAssosicatedArtifactsArePublished(persistedPublished);
    }
    
    private File.Builder randomFile() {
        return File.builder()
                   .withName(randomString())
                   .withAdministrativeAgreement(NOT_ADMINISTRATIVE_AGREEMENT)
                   .withMimeType(randomString())
                   .withSize(randomInteger().longValue())
                   .withEmbargoDate(randomInstant())
                   .withLicense(getCcByLicense())
                   .withIdentifier(UUID.randomUUID())
                   .withPublisherAuthority(randomBoolean());
    }
    
    public static License getCcByLicense() {
        var ccByUri = URI.create("https://creativecommons.org/licenses/by/4.0/");
        return new License.Builder()
                   .withIdentifier("CC_BY")
                   .withLabels(Map.of("en", "CC-BY 4.0"))
                   .withLink(ccByUri)
                   .build();
    }
    
    private Builder draftPublicationWithoutDoi() {
        return randomPublication().copy().withStatus(DRAFT);
    }
    
    private Publication createPersistedPublicationWithoutDoi() {
        var publication = randomPublication().copy().withDoi(null).build();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private Publication createPersistedPublicationWithoutDoi(Publication publication) {
        var withoutDoi = publication.copy().withDoi(null).build();
        return resourceService.createPublication(UserInstance.fromPublication(withoutDoi), withoutDoi);
    }
    
    private Publication createPersistedPublicationWithDoi(ResourceService resourceService, Publication sampleResource) {
        return resourceService.createPublication(UserInstance.fromPublication(sampleResource), sampleResource);
    }
    
    private Publication createPersistedPublicationWithDoi() {
        return createPersistedPublicationWithDoi(resourceService, randomPublication());
    }
    
    private Publication createUnpublishablePublication() {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private Publication generatePublication() {
        return PublicationGenerator.publicationWithoutIdentifier();
    }
    
    private ListingResult<Entity> fetchRestOfDatabaseEntries(ListingResult<Entity> listingResult) {
        return resourceService.scanResources(BIG_PAGE, listingResult.getStartMarker());
    }
    
    private SortableIdentifier extractIdentifierFromFirstScanResult(ListingResult<Entity> listingResult) {
        return listingResult.getDatabaseEntries()
                   .stream()
                   .collect(SingletonCollector.collect())
                   .getIdentifier();
    }
    
    private ListingResult<Entity> fetchFirstDataEntry() {
        ListingResult<Entity> listingResult = ListingResult.empty();
        while (listingResult.isEmpty()) {
            listingResult = resourceService.scanResources(1, listingResult.getStartMarker());
        }
        return listingResult;
    }
    
    private Publication publishResource(Publication resource) throws ApiGatewayException {
        resourceService.publishPublication(UserInstance.fromPublication(resource), resource.getIdentifier());
        return resourceService.getPublication(resource);
    }
    
    private ResourceService resourceServiceThatDoesNotReceivePublicationUpdateAfterCreation(AmazonDynamoDB client) {
        when(client.getItem(any(GetItemRequest.class)))
            .thenReturn(new GetItemResult().withItem(Collections.emptyMap()));
        
        return new ResourceService(client, clock);
    }
    
    private void assertThatIdentifierEntryHasBeenCreated() {
        assertThatResourceAndIdentifierEntryExist();
    }
    
    private void assertThatResourceAndIdentifierEntryExist() {
        ScanResult result = client.scan(
            new ScanRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME));
        assertThat(result.getCount(), is(equalTo(2)));
    }
    
    private void assertThatTheEntriesHaveNotBeenDeleted() {
        assertThatResourceAndIdentifierEntryExist();
    }
    
    private void assertThatAllEntriesHaveBeenDeleted() {
        ScanResult result = client.scan(
            new ScanRequest().withTableName(DatabaseConstants.RESOURCES_TABLE_NAME));
        assertThat(result.getCount(), is(equalTo(0)));
    }
    
    private DoiRequest expectedDoiRequestAfterPublicationUpdate(Publication initialPublication,
                                                                DoiRequest initialDoiRequest,
                                                                Publication publicationUpdate,
                                                                DoiRequest updatedDoiRequest) {
    
        return DoiRequest.builder()
                   .withOwner(new User(initialPublication.getResourceOwner().getOwner()))
                   .withCustomerId(initialPublication.getPublisher().getId())
                   .withIdentifier(initialDoiRequest.getIdentifier())
                   .withCreatedDate(initialDoiRequest.getCreatedDate())
                   .withModifiedDate(updatedDoiRequest.getModifiedDate())
                   .withStatus(TicketStatus.PENDING)
                   .withResourceStatus(publicationUpdate.getStatus())
                   .withPublicationDetails(PublicationDetails.create(publicationUpdate))
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
    
    private DoiRequest createDoiRequest(Publication resource)
        throws ApiGatewayException {
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
    
    private void verifyThatTheResourceWasMovedFromtheDrafts(ResourceDao resourceDaoWithStatusDraft) {
        Optional<ResourceDao> expectedEmptyResult = searchForResource(resourceDaoWithStatusDraft);
        assertThat(expectedEmptyResult.isEmpty(), is(true));
    }
    
    private void assertThatResourceCanBeFoundInDraftResources(ResourceDao resourceDaoWithStatusDraft) {
        Optional<ResourceDao> savedResource = searchForResource(resourceDaoWithStatusDraft);
        assertThat(savedResource.isPresent(), is(true));
    }
    
    private ResourceDao queryObjectForPublishedResource(Publication resourceWithStatusDraft) {
        Resource resourceWithStatusPublished = Resource.fromPublication(resourceWithStatusDraft).copy()
                                                   .withStatus(PUBLISHED)
                                                   .build();
        return new ResourceDao(resourceWithStatusPublished);
    }
    
    private QueryResult queryForDraftResource(ResourceDao resourceDao) {
        
        return client.query(new QueryRequest()
                                .withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                                .withIndexName(DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
                                .withKeyConditions(resourceDao.fetchEntryByTypeCustomerStatusKey())
        );
    }
    
    private Optional<ResourceDao> parseResult(QueryResult result) {
        return result.getItems().stream()
                   .map(map -> parseAttributeValuesMap(map, ResourceDao.class))
                   .findAny();
    }
    
    private Publication createPublishedResource() throws ApiGatewayException {
        Publication resource = createPersistedPublicationWithoutDoi();
        publishResource(resource);
        return resourceService.getPublication(resource);
    }
    
    private ResourceService resourceServiceProvidingDuplicateIdentifiers(SortableIdentifier identifier) {
        Supplier<SortableIdentifier> duplicateIdSupplier = () -> identifier;
        return new ResourceService(client, clock, duplicateIdSupplier);
    }
    
    private void assertThatJsonProcessingErrorIsPropagatedUp(Class<JsonProcessingException> expectedExceptionClass,
                                                             Executable action) {
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(expectedExceptionClass)));
    }
    
    private Set<Publication> createSamplePublicationsOfSingleOwner(UserInstance userInstance) {
        return
            Stream.of(publicationWithIdentifier(), publicationWithIdentifier(), publicationWithIdentifier())
                .map(publication -> injectOwner(userInstance, publication))
                .map(attempt(res -> createPersistedPublicationWithDoi(resourceService, res)))
                .map(Try::orElseThrow)
                .collect(Collectors.toSet());
    }
    
    private Publication injectOwner(UserInstance userInstance, Publication publication) {
        return publication.copy()
                   .withResourceOwner(new ResourceOwner(userInstance.getUsername(), AFFILIATION_NOT_IMPORTANT))
                   .withPublisher(new Organization.Builder().withId(userInstance.getOrganizationUri()).build())
                   .build();
    }
    
    private Publication expectedUpdatedResource(Publication originalResource,
                                                Publication updatedResource,
                                                UserInstance expectedOwner) {
        return originalResource.copy().withResourceOwner(new ResourceOwner(expectedOwner.getUsername(),
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
        EntityDescription newEntityDescription =
            new EntityDescription.Builder().withMainTitle(UPDATED_TITLE).build();
        
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
        return attempt(() -> EntityDescription.class.getDeclaredField(MAIN_TITLE_FIELD).getName())
                   .orElseThrow(fail -> new RuntimeException(ENTITY_DESCRIPTION_DOES_NOT_HAVE_FIELD_ERROR));
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