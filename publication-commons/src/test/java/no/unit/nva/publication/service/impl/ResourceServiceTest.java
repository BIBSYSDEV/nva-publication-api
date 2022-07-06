package no.unit.nva.publication.service.impl;

import static java.util.Collections.emptyList;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.publicationWithIdentifier;
import static no.unit.nva.model.testing.PublicationGenerator.publicationWithoutIdentifier;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_BY_IDENTIFIER_NOT_FOUND_ERROR_PREFIX;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_FILE_SET_FIELD;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.publication.service.impl.UpdateResourceService.RESOURCE_LINK_FIELD;
import static no.unit.nva.publication.service.impl.UpdateResourceService.RESOURCE_WITHOUT_MAIN_TITLE_ERROR;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
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
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
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

public class ResourceServiceTest extends ResourcesLocalTest {

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
    public static final URI SOME_DOI = URI.create("https://some-doi.example.org");
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final int BIG_PAGE = 10;
    public static final URI UNIMPORTANT_AFFILIATION = null;
    public static final URI AFFILIATION_NOT_IMPORTANT = null;
    private static final URI SOME_ORG = randomUri();
    public static final UserInstance SAMPLE_USER = UserInstance.create(randomString(), SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
    private static final Instant RESOURCE_CREATION_TIME = Instant.parse("1900-12-03T10:15:30.00Z");
    private static final Instant RESOURCE_MODIFICATION_TIME = Instant.parse("2000-01-03T00:00:18.00Z");
    private static final Instant RESOURCE_SECOND_MODIFICATION_TIME = Instant.parse("2010-01-03T02:00:25.00Z");
    private static final Instant RESOURCE_THIRD_MODIFICATION_TIME = Instant.parse("2020-01-03T06:00:32.00Z");
    private static final URI SOME_LINK = URI.create("http://www.example.com/someLink");
    private ResourceService resourceService;
    private Clock clock;
    private DoiRequestService doiRequestService;
    private MessageService messageService;

    @BeforeEach
    public void init() {
        super.init();
        Clock clock = setupClock();
        resourceService = new ResourceService(client,  clock);
        doiRequestService = new DoiRequestService(client, clock);
        messageService = new MessageService(client, clock, SortableIdentifier::next);
    }

    public Optional<ResourceDao> searchForResource(ResourceDao resourceDaoWithStatusDraft) {
        QueryResult queryResult = queryForDraftResource(resourceDaoWithStatusDraft);
        return parseResult(queryResult);
    }

    @Test
    void createResourceWithPredefinedCreationDateStoresResourceWithCreationDateEqualToInputsCreationDate()
        throws TransactionFailedException, NotFoundException {
        Publication inputPublication = generatePublication();
        assertThat(inputPublication.getSubjects(), is(not(nullValue())));
        verifyThatResourceClockWillReturnPredefinedCreationTime();
        Instant publicationPredefinedTime = Instant.now();

        inputPublication.setCreatedDate(publicationPredefinedTime);
        SortableIdentifier savedPublicationIdentifier =
            resourceService.createPublicationWithPredefinedCreationDate(inputPublication).getIdentifier();
        Publication savedPublication = resourceService.getPublicationByIdentifier(savedPublicationIdentifier);

        // inject publicationIdentifier for making the inputPublication and the savedPublication equal.
        inputPublication.setIdentifier(savedPublicationIdentifier);
        Diff diff = JAVERS.compare(inputPublication, savedPublication);
        assertThat(publicationPredefinedTime, is(not(equalTo(RESOURCE_CREATION_TIME))));
        assertThat(diff.prettyPrint(), savedPublication, is(equalTo(inputPublication)));
    }

    @Test
    void createResourceWhilePersistingEntryFromLegacySystemsStoresResourceWithDatesEqualToEntryDates()
        throws TransactionFailedException, NotFoundException {
        Publication inputPublication = generatePublication();
        Instant predefinedPublishTime = Instant.now();

        inputPublication.setPublishedDate(predefinedPublishTime);
        inputPublication.setCreatedDate(RESOURCE_CREATION_TIME);
        inputPublication.setModifiedDate(RESOURCE_MODIFICATION_TIME);
        inputPublication.setStatus(PublicationStatus.PUBLISHED);

        SortableIdentifier savedPublicationIdentifier =
            resourceService
                .createPublicationFromImportedEntry(inputPublication)
                .getIdentifier();
        Publication savedPublication = resourceService.getPublicationByIdentifier(savedPublicationIdentifier);

        // inject publicationIdentifier for making the inputPublication and the savedPublication equal.
        inputPublication.setIdentifier(savedPublicationIdentifier);

        assertThat(savedPublication, is(equalTo(inputPublication)));
        assertThat(savedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void createResourceReturnsResourceWithCreatedAndModifiedDateSetByThePlatform() throws ApiGatewayException {

        Publication resource = generatePublication();

        UserInstance userInstance = UserInstance.fromPublication(resource);
        Publication savedResource = resourceService.createPublication(userInstance, resource);
        Publication readResource = resourceService.getPublication(savedResource);
        Publication expectedResource = expectedResourceFromSampleResource(resource, savedResource);

        assertThat(savedResource, is(equalTo(expectedResource)));
        assertThat(readResource, is(equalTo(expectedResource)));
        assertThat(readResource, is(not(sameInstance(expectedResource))));
    }

    @Test
    void createResourceThrowsTransactionFailedExceptionWhenResourceWithSameIdentifierExists()
        throws ApiGatewayException {
        final Publication sampleResource = randomPublication();
        final Publication collidingResource = sampleResource.copy()
            .withPublisher(anotherPublisher())
            .withResourceOwner(new ResourceOwner(SOME_OTHER_USER,null))
            .build();
        ResourceService resourceService = resourceServiceProvidingDuplicateIdentifiers(sampleResource.getIdentifier());

        createPublication(resourceService, sampleResource);
        Executable action = () -> createPublication(resourceService, collidingResource);
        assertThrows(TransactionFailedException.class, action);

        assertThat(sampleResource.getIdentifier(), is(equalTo(collidingResource.getIdentifier())));
        assertThat(sampleResource.getResourceOwner().getOwner(),
                   is(not(equalTo(collidingResource.getResourceOwner().getOwner()))));
        assertThat(sampleResource.getPublisher().getId(), is(not(equalTo(collidingResource.getPublisher().getId()))));
    }

    @Test
    void createResourceSavesResourcesWithSameOwnerAndPublisherButDifferentIdentifier()
        throws ApiGatewayException {
        final Publication sampleResource = publicationWithIdentifier();
        final Publication anotherResource = publicationWithIdentifier();

        createPublication(resourceService, sampleResource);
        assertDoesNotThrow(() -> createPublication(resourceService, anotherResource));
    }

    @Test
    void createPublicationReturnsNullWhenResourceDoesNotBecomeAvailable() throws ApiGatewayException {
        Publication publication = publicationWithoutIdentifier();
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);

        ResourceService resourceService = resourceServiceThatDoesNotReceivePublicationUpdateAfterCreation(client);
        Publication actualPublication = createPublication(resourceService, publication);
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
        Publication sampleResource = createSampleResourceWithDoi();
        UserInstance userInstance = UserInstance.fromPublication(sampleResource);
        Publication savedResource = resourceService.getPublication(userInstance, sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }

    @Test
    void whenPublicationOwnerIsUpdatedTheResourceEntryMaintainsTheRestResourceMetadata()
        throws ApiGatewayException {
        Publication sampleResource = createSampleResourceWithDoi();

        UserInstance oldOwner = UserInstance.fromPublication(sampleResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Publication newResource = resourceService.getPublication(newOwner, sampleResource.getIdentifier());

        Publication expectedResource = expectedUpdatedResource(sampleResource, newOwner);
        Diff diff = JAVERS.compare(expectedResource, newResource);
        assertThat(diff.prettyPrint(), newResource, is(equalTo(expectedResource)));
    }

    @Test
    void whenPublicationOwnerIsUpdatedThenBothOrganizationAndUserAreUpdated()
        throws ApiGatewayException {
        Publication originalResource = createSampleResourceWithDoi();
        UserInstance oldOwner = UserInstance.fromPublication(originalResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);

        Publication newResource = resourceService.getPublication(newOwner, originalResource.getIdentifier());

        assertThat(newResource.getOwner(), is(equalTo(newOwner.getUserIdentifier())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getOrganizationUri())));
    }

    @Test
    void whenPublicationOwnerIsUpdatedTheModifiedDateIsUpdated()
        throws ApiGatewayException {
        Publication sampleResource = createSampleResourceWithDoi();
        UserInstance oldOwner = UserInstance.fromPublication(sampleResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Publication newResource = resourceService.getPublication(newOwner, sampleResource.getIdentifier());

        assertThat(newResource.getModifiedDate(), is(equalTo(RESOURCE_MODIFICATION_TIME)));
    }

    @Test
    void resourceIsUpdatedWhenResourceUpdateIsReceived() throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();
        Publication actualOriginalResource = resourceService.getPublication(resource);
        assertThat(actualOriginalResource, is(equalTo(resource)));

        Publication resourceUpdate = updateResourceTitle(resource);
        resourceService.updatePublication(resourceUpdate);
        Publication actualUpdatedResource = resourceService.getPublication(resource);

        assertThat(actualUpdatedResource, is(equalTo(resourceUpdate)));
        assertThat(actualUpdatedResource, is(not(equalTo(actualOriginalResource))));
    }

    @Test
    void resourceUpdateFailsWhenUpdateChangesTheOwnerPartOfThePrimaryKey() throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();
        Publication resourceUpdate = updateResourceTitle(resource);
        resourceUpdate.setResourceOwner(new ResourceOwner(ANOTHER_OWNER, UNIMPORTANT_AFFILIATION));
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    void resourceUpdateFailsWhenUpdateChangesTheOrganizationPartOfThePrimaryKey()
        throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();
        Publication resourceUpdate = updateResourceTitle(resource);

        resourceUpdate.setPublisher(newOrganization());
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    void resourceUpdateFailsWhenUpdateChangesTheIdentifierPartOfThePrimaryKey()
        throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();
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

        ResourceService failingService = new ResourceService(client,  clock);

        Publication resource = publicationWithIdentifier();
        Executable action = () -> createPublication(failingService, resource);
        TransactionFailedException actualException = assertThrows(TransactionFailedException.class, action);
        Throwable actualCause = actualException.getCause();
        assertThat(actualCause.getMessage(), is(equalTo(expectedMessage)));
    }

    @Test
    void insertPreexistingPublicationIdentifierStoresPublicationInDatabaseWithoutChangingIdentifier()
        throws TransactionFailedException {
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

        ResourceService failingResourceService = new ResourceService(client,  clock);

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

        ResourceService failingResourceService = new ResourceService(client,  clock);

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

        ResourceService failingResourceService = new ResourceService(mockClient,  clock);
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

        ResourceService failingResourceService = new ResourceService(mockClient,  clock);
        Class<JsonProcessingException> expectedExceptionClass = JsonProcessingException.class;

        SortableIdentifier someIdentifier = SortableIdentifier.next();
        Executable action = () -> failingResourceService.getPublication(SAMPLE_USER, someIdentifier);

        assertThatJsonProcessingErrorIsPropagatedUp(expectedExceptionClass, action);
    }

    @Test
    void publishResourceSetsPublicationStatusToPublished()
        throws ApiGatewayException {

        Publication resource = createSampleResourceWithDoi();
        UserInstance userInstance = UserInstance.fromPublication(resource);
        resourceService.publishPublication(userInstance, resource.getIdentifier());
        Publication actualResource = resourceService.getPublication(resource);

        Publication expectedResource = resource.copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .build();

        assertThat(actualResource, is(equalTo(expectedResource)));
    }

    @Test
    void publishResourceReturnsUpdatedResource() throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();
        publishResource(resource);
        Publication updatedResource = resourceService.getPublication(resource);
        Publication expectedResource = resource.copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .build();

        assertThat(updatedResource, is(equalTo(expectedResource)));
    }

    @Test
    void publishPublicationReturnsResponseThatRequestWasAcceptedWhenResourceIsNotPublished()
        throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();

        UserInstance userInstance = UserInstance.fromPublication(resource);
        PublishPublicationStatusResponse response = resourceService.publishPublication(
            userInstance, resource.getIdentifier());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    void publishPublicationReturnsPublicationResponseThatNoActionWasTakenWhenResourceIsAlreadyPublished()
        throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();

        UserInstance userInstance = UserInstance.fromPublication(resource);
        resourceService.publishPublication(userInstance, resource.getIdentifier());
        PublishPublicationStatusResponse response = resourceService.publishPublication(
            userInstance, resource.getIdentifier());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NO_CONTENT)));
    }

    @Test
    void byTypeCustomerStatusIndexIsUpdatedWhenResourceIsUpdated() throws ApiGatewayException {
        Publication resourceWithStatusDraft = createSampleResourceWithDoi();
        ResourceDao resourceDaoWithStatusDraft = new ResourceDao(Resource.fromPublication(resourceWithStatusDraft));

        assertThatResourceCanBeFoundInDraftResources(resourceDaoWithStatusDraft);

        resourceService.publishPublication(UserInstance.fromPublication(resourceWithStatusDraft),
                                           resourceWithStatusDraft.getIdentifier());

        verifyThatTheResourceWasMovedFromtheDrafts(resourceDaoWithStatusDraft);

        verifyThatTheResourceIsInThePublishedResources(resourceWithStatusDraft);
    }

    @Test
    void publishPublicationSetsPublishedDate() throws ApiGatewayException {
        Publication updatedResource = createPublishedResource();
        assertThat(updatedResource.getPublishedDate(), is(equalTo(RESOURCE_MODIFICATION_TIME)));
    }

    @Test
    void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoTitle()
        throws ApiGatewayException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.getEntityDescription().setMainTitle(null);
        Publication savedResource = createPublication(resourceService, sampleResource);

        Executable action = () -> resourceService.publishPublication(UserInstance.fromPublication(sampleResource),
                                                                     savedResource.getIdentifier());

        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, containsString(RESOURCE_WITHOUT_MAIN_TITLE_ERROR));
    }

    @Test
    void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoLinkAndNoFiles()
        throws ApiGatewayException, NoSuchFieldException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(null);
        sampleResource.setFileSet(emptyFileSet());
        Publication savedResource = createPublication(resourceService, sampleResource);

        Executable action =
            () -> publishResource(savedResource);
        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();

        assertThat(actualMessage, containsString(InvalidPublicationException.ERROR_MESSAGE_TEMPLATE));
        assertThat(actualMessage, containsString(sampleResource.getClass()
                                                     .getDeclaredField(RESOURCE_LINK_FIELD).getName()));
        assertThat(actualMessage, containsString(sampleResource.getClass()
                                                     .getDeclaredField(RESOURCE_FILE_SET_FIELD).getName()));
    }

    @Test
    void publishResourcePublishesResourceWhenLinkIsPresentButNoFiles() throws ApiGatewayException {

        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(SOME_LINK);
        sampleResource.setFileSet(emptyFileSet());
        Publication savedResource = createPublication(resourceService, sampleResource);
        Publication updatedResource =
            publishResource(savedResource);
        assertThat(updatedResource.getStatus().toString(), is(equalTo(PublicationStatus.PUBLISHED.toString())));
    }

    @Test
    void publishResourcePublishesResourceWhenResourceHasFilesButNoLink() throws ApiGatewayException {

        Publication sampleResource = createSampleResourceWithDoi();
        sampleResource.setLink(null);

        Publication updatedResource =
            publishResource(sampleResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void publishResourceUpdatesResourceStatusInResourceWithDoiRequest()
        throws ApiGatewayException {
        Publication publication = createSampleResourceWithDoi();
        SortableIdentifier doiRequestIdentifier = createDoiRequest(publication).getIdentifier();
        publishResource(publication);

        UserInstance userInstance = UserInstance.fromPublication(publication);
        DoiRequest actualDoiRequest = doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);

        assertThat(actualDoiRequest.getResourceStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void markPublicationForDeletionThrowsExceptionWhenDeletingPublishedPublication() throws ApiGatewayException {
        Publication resource = createPublishedResource();
        Executable action =
            () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource), resource.getIdentifier());
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE));
        assertThat(exception.getMessage(), containsString(resource.getIdentifier().toString()));
    }

    @Test
    void markPublicationForDeletionLogsConditionExceptionWhenUpdateConditionFails() throws ApiGatewayException {
        TestAppender testAppender = LogUtils.getTestingAppender(ResourceService.class);
        Publication resource = createPublishedResource();
        Executable action =
            () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource), resource.getIdentifier());
        assertThrows(BadRequestException.class, action);
        assertThat(testAppender.getMessages(), containsString(ConditionalCheckFailedException.class.getName()));
    }

    @Test
    void createResourceReturnsNewIdentifierWhenResourceIsCreated() throws ApiGatewayException {
        Publication sampleResource = publicationWithoutIdentifier();
        Publication savedResource = createPublication(resourceService, sampleResource);
        assertThat(sampleResource.getIdentifier(), is(equalTo(null)));
        assertThat(savedResource.getIdentifier(), is(notNullValue()));
    }

    @Test
    void deletePublicationCanMarkDraftForDeletion() throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();

        Publication resourceUpdate =
            resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource), resource.getIdentifier());
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        Publication resourceForDeletion = resourceService.getPublication(resource);
        assertThat(resourceForDeletion.getStatus(),
                   Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    void markPublicationForDeletionReturnsUpdatedResourceCanMarkDraftForDeletion()
        throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();

        Publication resourceUpdate =
            resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource), resource.getIdentifier());
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    void markPublicationForDeletionResourceThrowsErrorWhenDeletingPublicationThatIsMarkedForDeletion()
        throws ApiGatewayException {
        Publication resource = createSampleResourceWithDoi();
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
        DoiRequestService doiRequestService = new DoiRequestService(client,  clock);
        Publication resource = createSampleResourceWithDoi();
        DoiRequest originalDoiRequest = createDoiRequest(resource);

        resource.getEntityDescription().setMainTitle(ANOTHER_TITLE);
        resourceService.updatePublication(resource);

        UserInstance userInstance = UserInstance.create(resource.getResourceOwner(), resource.getPublisher().getId());
        DoiRequest updatedDoiRequest = doiRequestService
            .getDoiRequestByResourceIdentifier(userInstance, resource.getIdentifier());

        DoiRequest expectedDoiRequest = originalDoiRequest.copy()
            .withResourceTitle(ANOTHER_TITLE)
            .build();
        assertThat(updatedDoiRequest, is(equalTo(expectedDoiRequest)));
        assertThat(updatedDoiRequest, doesNotHaveEmptyValues());
    }

    @Test
    void updateResourceUpdatesAllFieldsInDoiRequest()
        throws ApiGatewayException {
        DoiRequestService doiRequestService = new DoiRequestService(client,  clock);
        Publication initialPublication = createPublication(resourceService, PublicationGenerator.randomPublication());
        UserInstance userInstance = UserInstance.fromPublication(initialPublication);
        DoiRequest initialDoiRequest = createDoiRequest(initialPublication);

        Publication publicationUpdate = updateAllPublicationFieldsExpectIdentifierAndOwnerInfo(initialPublication);
        resourceService.updatePublication(publicationUpdate);

        DoiRequest updatedDoiRequest = doiRequestService
            .getDoiRequestByResourceIdentifier(userInstance,
                                               initialPublication.getIdentifier());

        DoiRequest expectedDoiRequest = expectedDoiRequestAfterPublicationUpdate(
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
    void updateResourceDoesNotCreateDoiRequestWhenItDoesNotPreexist()
        throws ApiGatewayException {
        DoiRequestService doiRequestService = new DoiRequestService(client,  clock);
        Publication resource = createSampleResourceWithDoi();
        UserInstance userInstance = UserInstance.create(resource.getOwner(), resource.getPublisher().getId());

        resource.getEntityDescription().setMainTitle(ANOTHER_TITLE);
        resourceService.updatePublication(resource);

        Executable action = () -> doiRequestService
            .getDoiRequestByResourceIdentifier(userInstance, resource.getIdentifier());

        assertThrows(NotFoundException.class, action);
    }

    @Test
    void deleteDraftPublicationDeletesDraftResourceWithoutDoi()
        throws ApiGatewayException {
        Publication publication = createSampleResourceWithoutDoi();
        assertThatIdentifierEntryHasBeenCreated();

        Executable fetchResourceAction = () -> resourceService.getPublication(publication);
        assertDoesNotThrow(fetchResourceAction);

        UserInstance userInstance = UserInstance.fromPublication(publication);
        resourceService.deleteDraftPublication(userInstance, publication.getIdentifier());
        assertThrows(NotFoundException.class, fetchResourceAction);

        assertThatAllEntriesHaveBeenDeleted();
    }

    @Test
    void deleteDraftPublicationThrowsExceptionWhenResourceHasDoi()
        throws ApiGatewayException {
        Publication publication = createSampleResourceWithDoi();
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
        Publication publication = createSampleResourceWithoutDoi();
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
        Publication publication = createSampleResourceWithoutDoi();
        createDoiRequest(publication);

        UserInstance userInstance = UserInstance.fromPublication(publication);
        resourceService.deleteDraftPublication(userInstance, publication.getIdentifier());

        assertThatAllEntriesHaveBeenDeleted();
    }

    @Test
    void getResourceByIdentifierReturnsExistingResource() throws ApiGatewayException {
        Publication resource = createSampleResourceWithoutDoi();

        Publication retrievedResource = resourceService.getPublicationByIdentifier(resource.getIdentifier());
        assertThat(retrievedResource, is(equalTo(resource)));
    }

    @Test
    void getResourceByIdentifierThrowsNotFoundExceptionWhenResourceDoesNotExist() {
        TestAppender testAppender = LogUtils.getTestingAppender(ReadResourceService.class);
        SortableIdentifier someIdentifier = SortableIdentifier.next();
        Executable action = () -> resourceService.getPublicationByIdentifier(someIdentifier);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(someIdentifier.toString()));
        assertThat(testAppender.getMessages(), containsString(RESOURCE_BY_IDENTIFIER_NOT_FOUND_ERROR_PREFIX));
        assertThat(testAppender.getMessages(), containsString(someIdentifier.toString()));
    }

    @Test
    void shouldScanEntriesInDatabaseAfterSpecifiedMarker() throws ApiGatewayException {
        var samplePublication = createPublication(resourceService, PublicationGenerator.randomPublication());
        var sampleDoiRequestIdentifier = doiRequestService.createDoiRequest(samplePublication);
        var userInstance = UserInstance.create(samplePublication.getOwner(), samplePublication.getPublisher().getId());
        var sampleMessageIdentifier = messageService.createSimpleMessage(userInstance, samplePublication,
                                                                         randomString());

        var firstListingResult = fetchFirstDataEntry();
        var identifierInFirstScan = extractIdentifierFromFirstScanResult(firstListingResult);

        var secondListingResult = fetchRestOfDatabaseEntries(firstListingResult);
        var identifiersFromSecondScan = secondListingResult
            .getDatabaseEntries().stream()
            .map(DataEntry::getIdentifier)
            .collect(Collectors.toList());

        var expectedIdentifiers =
            new ArrayList<>(List.of(samplePublication.getIdentifier(),
                                    sampleDoiRequestIdentifier,
                                    sampleMessageIdentifier)
            );
        expectedIdentifiers.remove(identifierInFirstScan);
        assertThat(identifiersFromSecondScan,
                   containsInAnyOrder(expectedIdentifiers.toArray(SortableIdentifier[]::new)));
    }

    @Test
    void shouldUpdateResourceRowVersionWhenEntityIsRefreshed() {
        int arbitraryNumberOfResources = 40;
        int numberOfTotalExpectedDatabaseEntries = 2 * arbitraryNumberOfResources; //due to identity entries
        var sampleResources = createManySampleResources(arbitraryNumberOfResources);

        resourceService.refreshResources(sampleResources);
        var firstUpdates =
            resourceService.scanResources(numberOfTotalExpectedDatabaseEntries, null).getDatabaseEntries();

        resourceService.refreshResources(sampleResources);
        var secondUpdates =
            resourceService.scanResources(numberOfTotalExpectedDatabaseEntries, null).getDatabaseEntries();

        for (var firstUpdate : firstUpdates) {
            DataEntry secondUpdate = findMatchingSecondUpdate(secondUpdates, firstUpdate);
            assertThat(secondUpdate.getRowVersion(), is(not(equalTo(firstUpdate.getRowVersion()))));
        }
    }

    @Test
    void shouldLogUserInformationQueryObjectAndResourceIdentifierWhenFailingToPublishResource()
        throws ApiGatewayException {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var sampleResource = createSampleResourceWithDoi();
        UserInstance userInstance = UserInstance.create(sampleResource.getOwner(), null);
        assertThrows(RuntimeException.class,
                     () -> resourceService.publishPublication(userInstance, sampleResource.getIdentifier()));
        assertThat(logger.getMessages(), containsString(userInstance.getUserIdentifier()));
        assertThat(logger.getMessages(), containsString(sampleResource.getIdentifier().toString()));
    }

    private Publication generatePublication() {
        var publication = PublicationGenerator.publicationWithoutIdentifier();
        publication.setDoiRequest(null);
        return publication;
    }

    private Publication createPublication(ResourceService resourceService, Publication sampleResource)
        throws ApiGatewayException {
        UserInstance userInstance = UserInstance.fromPublication(sampleResource);
        return resourceService.createPublication(userInstance, sampleResource);
    }

    private DataEntry findMatchingSecondUpdate(List<DataEntry> secondUpdates, DataEntry firstUpdate) {
        return secondUpdates.stream()
            .filter(resource -> resource.getIdentifier().equals(firstUpdate.getIdentifier()))
            .collect(SingletonCollector.collect());
    }

    private List<DataEntry> createManySampleResources(int numberOfResources) {
        return IntStream.range(0, numberOfResources)
            .boxed()
            .map(ignored -> PublicationGenerator.randomPublication())
            .map(attempt(p -> createPublication(resourceService, p)))
            .map(Try::orElseThrow)
            .map(Resource::fromPublication)
            .map(resource -> (DataEntry) resource)
            .collect(Collectors.toList());
    }

    private ListingResult<DataEntry> fetchRestOfDatabaseEntries(ListingResult<DataEntry> listingResult) {
        return resourceService.scanResources(BIG_PAGE, listingResult.getStartMarker());
    }

    private SortableIdentifier extractIdentifierFromFirstScanResult(ListingResult<DataEntry> listingResult) {
        return listingResult.getDatabaseEntries()
            .stream()
            .collect(SingletonCollector.collect())
            .getIdentifier();
    }

    private ListingResult<DataEntry> fetchFirstDataEntry() {
        ListingResult<DataEntry> listingResult = ListingResult.empty();
        while (listingResult.isEmpty()) {
            listingResult = resourceService.scanResources(1, listingResult.getStartMarker());
        }
        return listingResult;
    }

    private Publication publishResource(Publication resource) throws ApiGatewayException {
        resourceService.publishPublication(UserInstance.fromPublication(resource), resource.getIdentifier());
        return resourceService.getPublication(resource);
    }

    private Clock setupClock() {
        Clock clock = mock(Clock.class);
        when(clock.instant())
            .thenReturn(RESOURCE_CREATION_TIME)
            .thenReturn(RESOURCE_MODIFICATION_TIME)
            .thenReturn(RESOURCE_SECOND_MODIFICATION_TIME)
            .thenReturn(RESOURCE_THIRD_MODIFICATION_TIME);
        this.clock = clock;
        return clock;
    }

    private void verifyThatResourceClockWillReturnPredefinedCreationTime() {
        assertThat(clock.instant(), is(equalTo(RESOURCE_CREATION_TIME)));
        clock = setupClock();
    }

    private ResourceService resourceServiceThatDoesNotReceivePublicationUpdateAfterCreation(AmazonDynamoDB client) {
        when(client.getItem(any(GetItemRequest.class)))
            .thenReturn(new GetItemResult().withItem(Collections.emptyMap()));

        return new ResourceService(client,  clock);
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
            .withOwner(initialPublication.getOwner())
            .withCustomerId(initialPublication.getPublisher().getId())
            .withResourceIdentifier(initialPublication.getIdentifier())
            .withIdentifier(updatedDoiRequest.getIdentifier())
            .withCreatedDate(initialDoiRequest.getCreatedDate())
            .withModifiedDate(initialDoiRequest.getModifiedDate())
            .withDoi(publicationUpdate.getDoi())
            .withStatus(DoiRequestStatus.REQUESTED)
            .withResourceTitle(publicationUpdate.getEntityDescription().getMainTitle())
            .withResourceStatus(publicationUpdate.getStatus())
            .withResourceModifiedDate(publicationUpdate.getModifiedDate())
            .withResourcePublicationDate(publicationUpdate.getEntityDescription().getDate())
            .withResourcePublicationYear(publicationUpdate.getEntityDescription().getDate().getYear())
            .withContributors(publicationUpdate.getEntityDescription().getContributors())
            .withResourcePublicationInstance(
                publicationUpdate.getEntityDescription().getReference().getPublicationInstance())
            .withRowVersion(updatedDoiRequest.getRowVersion())
            .build();
    }

    private Publication updateAllPublicationFieldsExpectIdentifierAndOwnerInfo(Publication existingPublication) {
        return PublicationGenerator.randomPublication()
            .copy()
            .withIdentifier(existingPublication.getIdentifier())
            .withPublisher(existingPublication.getPublisher())
            .withResourceOwner(existingPublication.getResourceOwner())
            .build();
    }

    private DoiRequest createDoiRequest(Publication resource)
        throws BadRequestException, TransactionFailedException, NotFoundException {
        UserInstance userInstance = UserInstance.fromPublication(resource);
        doiRequestService.createDoiRequest(userInstance, resource.getIdentifier());
        return doiRequestService.getDoiRequestByResourceIdentifier(userInstance, resource.getIdentifier());
    }

    private void verifyThatTheResourceIsInThePublishedResources(Publication resourceWithStatusDraft) {
        ResourceDao resourceDaoWithStatusPublished = queryObjectForPublishedResource(resourceWithStatusDraft);

        Optional<ResourceDao> publishedResource = searchForResource(resourceDaoWithStatusPublished);
        assertThat(publishedResource.isPresent(), is(true));

        ResourceDao actualResourceDao = publishedResource.orElseThrow();
        assertThat(actualResourceDao.getData().getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
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
            .withStatus(PublicationStatus.PUBLISHED)
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
        Publication resource = createSampleResourceWithDoi();
        publishResource(resource);
        return resourceService.getPublication(resource);
    }

    private Publication expectedResourceFromSampleResource(Publication sampleResource, Publication savedResource) {

        return sampleResource.copy()
            .withIdentifier(savedResource.getIdentifier())
            .withPublisher(organizationWithoutLabels(sampleResource))
            .withResourceOwner(sampleResource.getResourceOwner())
            .withCreatedDate(RESOURCE_CREATION_TIME)
            .withModifiedDate(RESOURCE_CREATION_TIME)
            .withStatus(PublicationStatus.DRAFT)
            .build();
    }

    private Organization organizationWithoutLabels(Publication sampleResource) {
        return new Organization.Builder().withId(sampleResource.getPublisher().getId()).build();
    }

    private ResourceService resourceServiceProvidingDuplicateIdentifiers(SortableIdentifier identifier) {
        Supplier<SortableIdentifier> duplicateIdSupplier = () -> identifier;
        return new ResourceService(client, clock, duplicateIdSupplier);
    }

    private FileSet emptyFileSet() {
        return new FileSet(emptyList());
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
                .map(attempt(res -> createPublication(resourceService, res)))
                .map(Try::orElseThrow)
                .collect(Collectors.toSet());
    }

    private Publication injectOwner(UserInstance userInstance, Publication publication) {
        return publication.copy()
            .withResourceOwner(new ResourceOwner(userInstance.getUserIdentifier(), AFFILIATION_NOT_IMPORTANT))
            .withPublisher(new Organization.Builder().withId(userInstance.getOrganizationUri()).build())
            .build();
    }

    private Publication expectedUpdatedResource(Publication sampleResource, UserInstance newOwner) {
        return sampleResource.copy()
            .withResourceOwner(new ResourceOwner(newOwner.getUserIdentifier(), AFFILIATION_NOT_IMPORTANT))
            .withPublisher(userOrganization(newOwner))
            .withCreatedDate(sampleResource.getCreatedDate())
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .build();
    }

    private Publication createSampleResourceWithoutDoi() throws ApiGatewayException {
        var originalResource = publicationWithIdentifier().copy().withDoi(null).build();
        return createPublication(resourceService, originalResource);
    }

    private Publication createSampleResourceWithDoi() throws ApiGatewayException {
        var originalResource = publicationWithIdentifier();
        originalResource.setDoi(SOME_DOI);
        return createPublication(resourceService, originalResource);
    }

    private void assertThatUpdateFails(Publication resourceUpdate) {
        Executable action = () -> resourceService.updatePublication(resourceUpdate);
        TransactionFailedException exception = assertThrows(TransactionFailedException.class, action);
        Throwable cause = exception.getCause();
        assertThat(cause, instanceOf(TransactionCanceledException.class));
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