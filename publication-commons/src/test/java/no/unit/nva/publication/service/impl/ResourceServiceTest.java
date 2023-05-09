package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.*;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.model.business.ImportStatus;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static java.util.Collections.emptyList;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.*;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
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
    private static final String ASSIGNEE1 = "assignee";
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
    void shouldKeepImportedDataCreationDates() throws NotFoundException {
        var randomInstant = RandomDataGenerator.randomInstant();
        var inputPublication = randomPublication().copy().withCreatedDate(randomInstant).build();

        var savedPublicationIdentifier = resourceService.createPublicationWithPredefinedCreationDate(inputPublication)
                                             .getIdentifier();
        var savedPublication = resourceService.getPublicationByIdentifier(savedPublicationIdentifier);

        // inject publicationIdentifier for making the inputPublication and the savedPublication equal.
        inputPublication.setIdentifier(savedPublicationIdentifier);

        var possiblyErrorDiff = JAVERS.compare(inputPublication, savedPublication);
        assertThat(possiblyErrorDiff.prettyPrint(), savedPublication, is(equalTo(inputPublication)));
    }

    @Test
    void shouldKeepImportedEntryCreationAndModifiedDates() throws NotFoundException {
        var createdDate = randomInstant();
        var modifiedDate = randomInstant();
        var inputPublication = randomPublication().copy()
                                   .withCreatedDate(createdDate)
                                   .withModifiedDate(modifiedDate)
                                   .withStatus(PUBLISHED)
                                   .build();
        var savedPublicationIdentifier = resourceService.createPublicationFromImportedEntry(inputPublication)
                                             .getIdentifier();
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
    void createResourceSavesResourcesWithSameOwnerAndPublisherButDifferentIdentifier() throws BadRequestException {
        final Publication sampleResource = publicationWithIdentifier();
        final Publication anotherResource = publicationWithIdentifier();

        createPersistedPublicationWithDoi(resourceService, sampleResource);
        assertDoesNotThrow(() -> createPersistedPublicationWithDoi(resourceService, anotherResource));
    }

    @Test
    void createPublicationReturnsNullWhenResourceDoesNotBecomeAvailable() throws BadRequestException {
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
    void getResourceByIdentifierReturnsResourceWhenResourceExists() throws ApiGatewayException {
        Publication sampleResource = createPersistedPublicationWithDoi();
        UserInstance userInstance = UserInstance.fromPublication(sampleResource);
        Publication savedResource = resourceService.getPublication(userInstance, sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }

    @Test
    void whenPublicationOwnerIsUpdatedTheResourceEntryMaintainsTheRestResourceMetadata() throws ApiGatewayException {
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
    void whenPublicationOwnerIsUpdatedThenBothOrganizationAndUserAreUpdated() throws ApiGatewayException {
        Publication originalResource = createPersistedPublicationWithDoi();
        UserInstance oldOwner = UserInstance.fromPublication(originalResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);

        Publication newResource = resourceService.getPublication(newOwner, originalResource.getIdentifier());

        assertThat(newResource.getResourceOwner().getOwner().getValue(), is(equalTo(newOwner.getUsername())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getOrganizationUri())));
    }

    @Test
    void whenPublicationOwnerIsUpdatedTheModifiedDateIsUpdated() throws ApiGatewayException {
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
        var client = mock(AmazonDynamoDB.class);
        var expectedMessage = new RuntimeException("expectedMessage");
        when(client.getItem(any(GetItemRequest.class))).thenThrow(expectedMessage);
        var resource = publicationWithIdentifier();

        var failingResourceService = new ResourceService(client, clock);

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
        Set<Publication> publicationsWithCristinIdentifier =
            createSamplePublicationsOfSingleCristinIdentifier(cristinIdentifier);
        List<Publication> actualPublication = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);
        HashSet<Publication> actualResourcesSet = new HashSet<>(actualPublication);
        assertThat(actualResourcesSet,
                   containsInAnyOrder(
                       publicationsWithCristinIdentifier.toArray(Publication[]::new)));
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
        publication.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", cristinIdentifier)));
        return publication;
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
        QueryResult responseWithInvalidItem = new QueryResult().withItems(
            List.of(ItemUtils.toAttributeValues(invalidItem)));
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
    void shouldPublishResourceWhenClientRequestsToPublish() throws ApiGatewayException {
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
    void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoLinkNoFilesAndNoDoi()
        throws BadRequestException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(null);
        sampleResource.getEntityDescription().getReference().setDoi(null);
        sampleResource.setAssociatedArtifacts(createEmptyArtifactList());
        Publication savedResource = createPersistedPublicationWithoutDoi(sampleResource);

        Executable action = () -> publishResource(savedResource);
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
    void markPublicationForDeletionLogsConditionExceptionWhenUpdateConditionFails() throws ApiGatewayException {
        TestAppender testAppender = LogUtils.getTestingAppender(ResourceService.class);
        Publication resource = createPublishedResource();
        Executable action = () -> resourceService.markPublicationForDeletion(UserInstance.fromPublication(resource),
                                                                             resource.getIdentifier());
        assertThrows(BadRequestException.class, action);
        assertThat(testAppender.getMessages(), containsString(ConditionalCheckFailedException.class.getName()));
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
    void updateResourceUpdatesLinkedDoiRequestUponUpdate() throws ApiGatewayException {
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
        assertThat(updatedDoiRequest, doesNotHaveEmptyValuesIgnoringFields(Set.of(ASSIGNEE1, FINALIZED_BY,
                                                                                  FINALIZED_DATE)));
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

        assertThat(updatedDoiRequest, doesNotHaveEmptyValuesIgnoringFields(Set.of(ASSIGNEE, FINALIZED_BY,
                                                                                  FINALIZED_DATE)));
        assertThat(expectedDoiRequest, doesNotHaveEmptyValuesIgnoringFields(Set.of(ASSIGNEE, FINALIZED_BY,
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
                                            .collect(Collectors.toList());

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
    void shouldCreateResourceFromImportCandidate() {
        var importCandidate = randomImportCandidate();
        resourceService.createImportCandidateFromImportedEntry(importCandidate);
        Resource.fromImportCandidate(importCandidate);
        assertThat(true, is(equalTo(true)));
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                .withStatus(PublicationStatus.PUBLISHED)
                .withImportStatus(ImportStatus.NOT_IMPORTED)
                .withLink(randomUri())
                .withDoi(randomDoi())
                .withIndexedDate(Instant.now())
                .withPublishedDate(Instant.now())
                .withHandle(randomUri())
                .withModifiedDate(Instant.now())
                .withCreatedDate(Instant.now())
                .withPublisher(new Organization.Builder().withId(randomUri()).build())
                .withSubjects(List.of(randomUri()))
                .withIdentifier(SortableIdentifier.next())
                .withRightsHolder(randomString())
                .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                .withFundings(List.of())
                .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                .withAssociatedArtifacts(List.of())
                .build();

    }

    private static AssociatedArtifactList createEmptyArtifactList() {
        return new AssociatedArtifactList(emptyList());
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
        return PublicationGenerator.publicationWithoutIdentifier();
    }

    private ListingResult<Entity> fetchRestOfDatabaseEntries(ListingResult<Entity> listingResult) {
        return resourceService.scanResources(BIG_PAGE, listingResult.getStartMarker());
    }

    private SortableIdentifier extractIdentifierFromFirstScanResult(ListingResult<Entity> listingResult) {
        return listingResult.getDatabaseEntries().stream().collect(SingletonCollector.collect()).getIdentifier();
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
        when(client.getItem(any(GetItemRequest.class))).thenReturn(
            new GetItemResult().withItem(Collections.emptyMap()));

        return new ResourceService(client, clock);
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
                   .withPublisher(new Organization.Builder().withId(userInstance.getOrganizationUri()).build())
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