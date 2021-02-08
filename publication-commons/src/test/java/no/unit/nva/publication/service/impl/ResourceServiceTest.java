package no.unit.nva.publication.service.impl;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.PublicationGenerator.publicationWithIdentifier;
import static no.unit.nva.publication.PublicationGenerator.publicationWithoutIdentifier;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_FILE_SET_FIELD;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_LINK_FIELD;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_WITHOUT_MAIN_TITLE_ERROR;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
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
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.service.impl.exceptions.ResourceCannotBeDeletedException;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.hamcrest.Matchers;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class ResourceServiceTest extends ResourcesDynamoDbLocalTest {

    public static final String ANOTHER_OWNER = "another@owner.no";
    public static final String SOME_OTHER_USER = "some_other@user.no";

    public static final String UPDATED_TITLE = "UpdatedTitle";
    public static final String SOME_INVALID_FIELD = "someInvalidField";
    public static final String SOME_STRING = "someValue";
    public static final SortableIdentifier SOME_IDENTIFIER = SortableIdentifier.next();
    public static final String MAIN_TITLE_FIELD = "mainTitle";
    public static final String ENTITY_DESCRIPTION_DOES_NOT_HAVE_FIELD_ERROR = EntityDescription.class.getName()
                                                                              + " does not have a field"
                                                                              + MAIN_TITLE_FIELD;
    public static final String ANOTHER_TITLE = "anotherTitle";
    public static final URI SOME_DOI = URI.create("https://some-doi.example.org");
    private static final URI SOME_ORG = URI.create(PublicationGenerator.PUBLISHER_ID);
    public static final UserInstance SAMPLE_USER = new UserInstance(PublicationGenerator.OWNER, SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
    private static final Instant RESOURCE_CREATION_TIME = Instant.parse("1900-12-03T10:15:30.00Z");
    private static final Instant RESOURCE_MODIFICATION_TIME = Instant.parse("2000-01-03T00:00:18.00Z");
    private static final Instant RESOURCE_SECOND_MODIFICATION_TIME = Instant.parse("2010-01-03T02:00:25.00Z");
    private static final Instant RESOURCE_THIRD_MODIFICATION_TIME = Instant.parse("2020-01-03T06:00:32.00Z");
    private static final URI SOME_LINK = URI.create("http://www.example.com/someLink");
    private final Javers javers = JaversBuilder.javers().build();
    private ResourceService resourceService;
    private Clock clock;

    @BeforeEach
    public void init() {
        super.init();
        clock = mock(Clock.class);
        when(clock.instant())
            .thenReturn(RESOURCE_CREATION_TIME)
            .thenReturn(RESOURCE_MODIFICATION_TIME)
            .thenReturn(RESOURCE_SECOND_MODIFICATION_TIME)
            .thenReturn(RESOURCE_THIRD_MODIFICATION_TIME);
        resourceService = new ResourceService(client, clock);
    }

    @Test
    public void createResourceCreatesResource() throws NotFoundException, ConflictException {

        Publication resource = publicationWithIdentifier();
        Publication savedResource = resourceService.createPublication(resource);
        Publication readResource = resourceService.getPublication(savedResource);
        Publication expectedResource = expectedResourceFromSampleResource(resource, savedResource);

        Diff diff = javers.compare(expectedResource, savedResource);
        assertThat(diff.prettyPrint(), diff.getChanges().size(), is(0));

        assertThat(savedResource, is(equalTo(expectedResource)));
        assertThat(readResource, is(equalTo(expectedResource)));
        assertThat(readResource, is(not(sameInstance(expectedResource))));
    }

    @Test
    public void createResourceThrowsConflictExceptionWhenResourceWithSameIdentifierExists() throws ConflictException {
        final Publication sampleResource = publicationWithIdentifier();
        final Publication collidingResource = sampleResource.copy()
            .withPublisher(anotherPublisher())
            .withOwner(ANOTHER_OWNER)
            .build();
        ResourceService resourceService = resourceServiceProvidingDuplicateIdentifiers();
        resourceService.createPublication(sampleResource);
        Executable action = () -> resourceService.createPublication(collidingResource);
        assertThrows(ConflictException.class, action);

        assertThat(sampleResource.getIdentifier(), is(equalTo(collidingResource.getIdentifier())));
        assertThat(sampleResource.getOwner(), is(not(equalTo(collidingResource.getOwner()))));
        assertThat(sampleResource.getPublisher().getId(), is(not(equalTo(collidingResource.getPublisher().getId()))));
    }

    @Test
    public void createResourceSavesResourcesWithSameOwnerAndPublisherButDifferentIdentifier()
        throws ConflictException {
        final Publication sampleResource = publicationWithIdentifier();
        final Publication anotherResource = publicationWithIdentifier();

        resourceService.createPublication(sampleResource);
        assertDoesNotThrow(() -> resourceService.createPublication(anotherResource));
    }

    @Test
    public void getResourceByIdentifierReturnsNotFoundWhenResourceDoesNotExist() {
        SortableIdentifier nonExistingIdentifier = SortableIdentifier.next();
        Executable action = () -> resourceService.getPublication(SAMPLE_USER, nonExistingIdentifier);
        assertThrows(NotFoundException.class, action);
    }

    @Test
    public void getResourceByIdentifierReturnsResourceWhenResourceExists()
        throws ApiGatewayException {
        Publication sampleResource = createSampleResource();
        Publication savedResource = resourceService.getPublication(SAMPLE_USER, sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedTheResourceEntryMaintainsTheRestResourceMetadata()
        throws ApiGatewayException {
        Publication sampleResource = createSampleResource();

        UserInstance oldOwner = extractUserInstance(sampleResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Publication newResource = resourceService.getPublication(newOwner, sampleResource.getIdentifier());

        Publication expectedResource = expectedUpdatedResource(sampleResource);

        assertThat(newResource, is(equalTo(expectedResource)));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedThenBothOrganizationAndUserAreUpdated()
        throws ApiGatewayException {
        Publication originalResource = createSampleResource();
        UserInstance oldOwner = extractUserInstance(originalResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier(), oldOwner, newOwner);

        Publication newResource = resourceService.getPublication(newOwner, originalResource.getIdentifier());

        assertThat(newResource.getOwner(), is(equalTo(newOwner.getUserIdentifier())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getOrganizationUri())));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedTheModifiedDateIsUpdated()
        throws ApiGatewayException {
        Publication sampleResource = createSampleResource();
        UserInstance oldOwner = extractUserInstance(sampleResource);
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Publication newResource = resourceService.getPublication(newOwner, sampleResource.getIdentifier());

        assertThat(newResource.getModifiedDate(), is(equalTo(RESOURCE_MODIFICATION_TIME)));
    }

    @Test
    public void resourceIsUpdatedWhenResourceUpdateIsReceived() throws ConflictException, NotFoundException {
        Publication resource = createSampleResource();
        Publication actualOriginalResource = resourceService.getPublication(resource);
        assertThat(actualOriginalResource, is(equalTo(resource)));

        Publication resourceUpdate = updateResourceTitle(resource);

        resourceService.updatePublication(resourceUpdate);
        Publication actualUpdatedResource = resourceService.getPublication(resource);

        assertThat(actualUpdatedResource, is(equalTo(resourceUpdate)));
        assertThat(actualUpdatedResource, is(not(equalTo(actualOriginalResource))));
    }

    @Test
    @DisplayName("resourceUpdate fails when Update changes the primary key (owner-part)")
    public void resourceUpdateFailsWhenUpdateChangesTheOwnerPartOfThePrimaryKey() throws ConflictException {
        Publication resource = createSampleResource();
        Publication resourceUpdate = updateResourceTitle(resource);

        resourceUpdate.setOwner(ANOTHER_OWNER);
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    @DisplayName("resourceUpdate fails when Update changes the primary key (organization-part)")
    public void resourceUpdateFailsWhenUpdateChangesTheOrganizationPartOfThePrimaryKey()
        throws ConflictException {
        Publication resource = createSampleResource();
        Publication resourceUpdate = updateResourceTitle(resource);

        resourceUpdate.setPublisher(newOrganization(SOME_OTHER_ORG));
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    @DisplayName("resourceUpdate fails when Update changes the primary key (primary-key-part)")
    public void resourceUpdateFailsWhenUpdateChangesTheIdentifierPartOfThePrimaryKey()
        throws ConflictException {
        Publication resource = createSampleResource();
        Publication resourceUpdate = updateResourceTitle(resource);

        resourceUpdate.setIdentifier(SortableIdentifier.next());
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    public void createResourceThrowsConflictExceptionWithInternalCauseWhenCreatingResourceFails() {
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        String expectedMessage = "expectedMessage";
        RuntimeException expectedCause = new RuntimeException(expectedMessage);
        when(client.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(expectedCause);

        ResourceService failingService = new ResourceService(client, clock);

        Publication resource = publicationWithIdentifier();
        Executable action = () -> failingService.createPublication(resource);
        ConflictException actualException = assertThrows(ConflictException.class, action);
        Throwable actualCause = actualException.getCause();
        assertThat(actualCause.getMessage(), is(equalTo(expectedMessage)));
    }

    @Test
    public void getResourcePropagatesExceptionWithWhenGettingResourceFailsForUnknownReason() {
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
    public void getResourcesByOwnerReturnsAllResourcesOwnedByUser() {
        Set<Publication> userResources = createSamplePublications();

        List<Publication> actualResources = resourceService.getResourcesByOwner(SAMPLE_USER);
        HashSet<Publication> actualResourcesSet = new HashSet<>(actualResources);

        assertThat(actualResourcesSet, is(equalTo(userResources)));
    }

    @Test
    public void getResourcesByOwnerReturnsEmptyListWhenUseHasNoPublications() {

        List<Publication> actualResources = resourceService.getResourcesByOwner(SAMPLE_USER);
        HashSet<Publication> actualResourcesSet = new HashSet<>(actualResources);

        assertThat(actualResourcesSet, is(equalTo(Collections.emptySet())));
    }

    @Test
    public void getResourcesByOwnerPropagatesExceptionWhenExceptionIsThrown() {
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        String expectedMessage = "expectedMessage";
        RuntimeException expectedException = new RuntimeException(expectedMessage);
        when(client.query(any(QueryRequest.class))).thenThrow(expectedException);

        ResourceService failingResourceService = new ResourceService(client, clock);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> failingResourceService.getResourcesByOwner(SAMPLE_USER));

        assertThat(exception.getMessage(), is(equalTo(expectedMessage)));
    }

    @Test
    public void getResourcesByOwnerPropagatesJsonProcessingExceptionWhenExceptionIsThrown() {
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        Item invalidItem = new Item().withString(SOME_INVALID_FIELD, SOME_STRING);
        QueryResult responseWithInvalidItem = new QueryResult().withItems(ItemUtils.toAttributeValues(invalidItem));
        when(mockClient.query(any(QueryRequest.class))).thenReturn(responseWithInvalidItem);

        ResourceService failingResourceService = new ResourceService(mockClient, clock);
        Class<JsonProcessingException> expectedExceptionClass = JsonProcessingException.class;

        assertThatJsonProcessingErrorIsPropagatedUp(expectedExceptionClass,
            () -> failingResourceService.getResourcesByOwner(SAMPLE_USER));
    }

    @Test
    public void getResourcePropagatesJsonProcessingExceptionWhenExceptionIsThrown() {

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
    public void publishResourceSetsPublicationStatusToPublished()
        throws ApiGatewayException {

        Publication resource = createSampleResource();
        UserInstance userInstance = extractUserInstance(resource);
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
    public void publishResourceReturnsUpdatedResource() throws ApiGatewayException {
        Publication resource = createSampleResource();
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
    public void publishPublicationReturnsResponseThatRequestWasAcceptedWhenResourceIsNotPublished()
        throws ApiGatewayException {
        Publication resource = createSampleResource();

        UserInstance userInstance = extractUserInstance(resource);
        PublishPublicationStatusResponse response = resourceService.publishPublication(
            userInstance, resource.getIdentifier());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    public void publishPublicationReturnsPublicationResponseThatNoActionWasTakenWhenResourceIsAlreadyPublished()
        throws ApiGatewayException {
        Publication resource = createSampleResource();

        UserInstance userInstance = extractUserInstance(resource);
        resourceService.publishPublication(userInstance, resource.getIdentifier());
        PublishPublicationStatusResponse response = resourceService.publishPublication(
            userInstance, resource.getIdentifier());

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NO_CONTENT)));
    }

    @Test
    public void byTypeCustomerStatusIndexIsUpdatedWhenResourceIsUpdated() throws ApiGatewayException {
        Publication resourceWithStatusDraft = createSampleResource();
        ResourceDao resourceDaoWithStatusDraft = new ResourceDao(Resource.fromPublication(resourceWithStatusDraft));

        assertThatResourceCanBeFoundInDraftResources(resourceDaoWithStatusDraft);

        resourceService.publishPublication(extractUserInstance(resourceWithStatusDraft),
            resourceWithStatusDraft.getIdentifier());

        verifyThatTheSampleHasStillStatusDraft(resourceWithStatusDraft);

        verifyThatTheResourceWasMovedFromtheDrafts(resourceDaoWithStatusDraft);

        verifyThatTheResourceIsInThePublishedResources(resourceWithStatusDraft);
    }

    public void verifyThatTheSampleHasStillStatusDraft(Publication resourceWithStatusDraft) {
        assertThat(resourceWithStatusDraft.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
    }

    public Optional<ResourceDao> searchForResource(ResourceDao resourceDaoWithStatusDraft) {
        QueryResult queryResult = queryForDraftResource(resourceDaoWithStatusDraft);
        return parseResult(queryResult);
    }

    @Test
    public void publishPublicationSetsPublishedDate() throws ApiGatewayException {
        Publication updatedResource = createPublishedResource();
        assertThat(updatedResource.getPublishedDate(), is(equalTo(RESOURCE_MODIFICATION_TIME)));
    }

    @Test
    public void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoTitle()
        throws ConflictException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.getEntityDescription().setMainTitle(null);
        Publication savedResource = resourceService.createPublication(sampleResource);

        Executable action = () ->
            resourceService.publishPublication(extractUserInstance(sampleResource), savedResource.getIdentifier());

        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, containsString(RESOURCE_WITHOUT_MAIN_TITLE_ERROR));
    }

    @Test
    public void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoLinkAndNoFiles()
        throws ConflictException, NoSuchFieldException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(null);
        sampleResource.setFileSet(emptyFileSet());
        Publication savedResource = resourceService.createPublication(sampleResource);

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
    public void publishResourcePublishesResourceWhenLinkIsPresentButNoFiles() throws ApiGatewayException {

        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(SOME_LINK);
        sampleResource.setFileSet(emptyFileSet());
        Publication savedResource = resourceService.createPublication(sampleResource);
        Publication updatedResource =
            publishResource(savedResource);
        assertThat(updatedResource.getStatus().toString(), is(equalTo(PublicationStatus.PUBLISHED.toString())));
    }

    public Publication publishResource(Publication resource) throws ApiGatewayException {
        resourceService.publishPublication(extractUserInstance(resource), resource.getIdentifier());
        return resourceService.getPublication(resource);
    }

    @Test
    public void publishResourcePublishesResourceWhenResourceHasFilesButNoLink() throws ApiGatewayException {

        Publication sampleResource = createSampleResource();
        sampleResource.setLink(null);

        Publication updatedResource =
            publishResource(sampleResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void publishResourceUpdatesResourceStatusInResourceWithDoiRequest()
        throws ApiGatewayException {
        Publication publication = createSampleResource();
        DoiRequestService doiRequestService = new DoiRequestService(client, clock);
        UserInstance userInstance = extractUserInstance(publication);
        SortableIdentifier doiRequestIdentifier = doiRequestService.createDoiRequest(userInstance,
            publication.getIdentifier());

        publishResource(publication);

        DoiRequest actualDoiRequest = doiRequestService.getDoiRequest(
            userInstance, doiRequestIdentifier);

        assertThat(actualDoiRequest.getResourceStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void deleteResourceThrowsExceptionWhenDeletingPublishedPublication() throws ApiGatewayException {
        Publication resource = createPublishedResource();
        Executable action =
            () -> resourceService.markPublicationForDeletion(extractUserInstance(resource), resource.getIdentifier());
        ResourceCannotBeDeletedException exception = assertThrows(ResourceCannotBeDeletedException.class, action);
        assertThat(exception.getMessage(), containsString(ResourceCannotBeDeletedException.DEFAULT_MESSAGE));
        assertThat(exception.getMessage(), containsString(resource.getIdentifier().toString()));
    }

    @Test
    public void createResourceReturnsNewIdentifierWhenResourceIsCreated() throws ConflictException {
        Publication sampleResource = publicationWithoutIdentifier();
        Publication savedResource = resourceService.createPublication(sampleResource);
        assertThat(sampleResource.getIdentifier(), is(equalTo(null)));
        assertThat(savedResource.getIdentifier(), is(notNullValue()));
    }

    @Test
    public void deletePublicationCanMarkDraftForDeletion() throws ApiGatewayException {
        Publication resource = createSampleResource();

        Publication resourceUpdate =
            resourceService.markPublicationForDeletion(extractUserInstance(resource), resource.getIdentifier());
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        Publication resourceForDeletion = resourceService.getPublication(resource);
        assertThat(resourceForDeletion.getStatus(),
            Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    public void deletePublicationReturnsUpdatedResourceCanMarkDraftForDeletion()
        throws ApiGatewayException {
        Publication resource = createSampleResource();

        Publication resourceUpdate =
            resourceService.markPublicationForDeletion(extractUserInstance(resource), resource.getIdentifier());
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    public void deleteResourceThrowsErrorWhenDeletingPublicationThatIsMarkedForDeletion()
        throws ApiGatewayException {
        Publication resource = createSampleResource();
        resourceService.markPublicationForDeletion(extractUserInstance(resource), resource.getIdentifier());
        Publication actualResource = resourceService.getPublication(resource);
        assertThat(actualResource.getStatus(), is(equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        Executable action = () -> resourceService.markPublicationForDeletion(extractUserInstance(resource),
            resource.getIdentifier());
        assertThrows(ResourceCannotBeDeletedException.class, action);
    }

    @Test
    public void updateResourceUpdatesLinkedDoiRequestUponUpdate()
        throws ConflictException, BadRequestException, NotFoundException {
        DoiRequestService doiRequestService = new DoiRequestService(client, clock);
        Publication resource = createSampleResource();
        UserInstance userInstance = new UserInstance(resource.getOwner(), resource.getPublisher().getId());
        DoiRequest originalDoiRequest = createDoiRequest(doiRequestService, resource, userInstance);

        resource.getEntityDescription().setMainTitle(ANOTHER_TITLE);
        resourceService.updatePublication(resource);

        DoiRequest updatedDoiRequest = doiRequestService
            .getDoiRequestByResourceIdentifier(userInstance, resource.getIdentifier());

        DoiRequest expectedDoiRequest = originalDoiRequest.copy()
            .withResourceTitle(ANOTHER_TITLE)
            .build();
        assertThat(updatedDoiRequest, is(equalTo(expectedDoiRequest)));
        assertThat(updatedDoiRequest, doesNotHaveEmptyValues());
    }

    @Test
    public void updateResourceUpdatesAllFieldsInDoiRequest()
        throws ConflictException, BadRequestException, NotFoundException {
        DoiRequestService doiRequestService = new DoiRequestService(client, clock);
        Publication emptyPublication =
            resourceService.createPublication(PublicationGenerator.generateEmptyPublication());
        doiRequestService.createDoiRequest(extractUserInstance(emptyPublication), emptyPublication.getIdentifier());

        DoiRequest initialDoiRequest = doiRequestService
            .getDoiRequestByResourceIdentifier(extractUserInstance(emptyPublication), emptyPublication.getIdentifier());

        Publication publicationUpdate = publicationWithAllDoiRequestRelatedFields(emptyPublication);
        resourceService.updatePublication(publicationUpdate);

        DoiRequest updatedDoiRequest = doiRequestService.getDoiRequestByResourceIdentifier(
            extractUserInstance(emptyPublication),
            emptyPublication.getIdentifier());

        DoiRequest expectedDoiRequest = DoiRequest.builder()
            .withIdentifier(updatedDoiRequest.getIdentifier())
            .withResourceIdentifier(emptyPublication.getIdentifier())
            .withDoi(publicationUpdate.getDoi())
            .withOwner(emptyPublication.getOwner())
            .withCreatedDate(initialDoiRequest.getCreatedDate())
            .withModifiedDate(initialDoiRequest.getModifiedDate())
            .withCustomerId(emptyPublication.getPublisher().getId())
            .withStatus(DoiRequestStatus.REQUESTED)
            .withResourceTitle(publicationUpdate.getEntityDescription().getMainTitle())
            .withResourceStatus(publicationUpdate.getStatus())
            .withResourceModifiedDate(publicationUpdate.getModifiedDate())
            .withResourcePublicationDate(publicationUpdate.getEntityDescription().getDate())
            .withResourcePublicationYear(publicationUpdate.getEntityDescription().getDate().getYear())
            .withResourcePublicationInstance(
                publicationUpdate.getEntityDescription().getReference().getPublicationInstance())
            .build();

        assertThat(expectedDoiRequest, doesNotHaveEmptyValues());
        assertThat(updatedDoiRequest, doesNotHaveEmptyValues());
        Diff diff = javers.compare(updatedDoiRequest, expectedDoiRequest);
        assertThat(diff.prettyPrint(), updatedDoiRequest, is(equalTo(expectedDoiRequest)));
    }

    @Test
    public void updateResourceDoesNotCreateDoiRequestWhenItDoesNotPreexist()
        throws ConflictException {
        DoiRequestService doiRequestService = new DoiRequestService(client, clock);
        Publication resource = createSampleResource();
        UserInstance userInstance = new UserInstance(resource.getOwner(), resource.getPublisher().getId());

        resource.getEntityDescription().setMainTitle(ANOTHER_TITLE);
        resourceService.updatePublication(resource);

        Executable action = () -> doiRequestService
            .getDoiRequestByResourceIdentifier(userInstance, resource.getIdentifier());

        assertThrows(NotFoundException.class, action);
    }

    private Publication publicationWithAllDoiRequestRelatedFields(Publication emptyPublication) {
        Publication publicationUpdate = PublicationGenerator.generatePublication(emptyPublication.getIdentifier());
        publicationUpdate.setDoi(SOME_DOI);
        return publicationUpdate;
    }

    private DoiRequest createDoiRequest(DoiRequestService doiRequestService, Publication resource,
                                        UserInstance userInstance)
        throws BadRequestException, ConflictException, NotFoundException {
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
            .withKeyConditions(resourceDao.byTypeCustomerStatusKey())
        );
    }

    private Optional<ResourceDao> parseResult(QueryResult result) {
        return result.getItems().stream()
            .map(map -> ResourceServiceUtils.parseAttributeValuesMap(map, ResourceDao.class))
            .findAny();
    }

    private Publication createPublishedResource() throws ApiGatewayException {
        Publication resource = createSampleResource();
        publishResource(resource);
        return resourceService.getPublication(resource);
    }

    private UserInstance extractUserInstance(Publication resource) {
        return new UserInstance(resource.getOwner(), resource.getPublisher().getId());
    }

    private Publication expectedResourceFromSampleResource(Publication sampleResource, Publication savedResource) {

        return sampleResource.copy()
            .withIdentifier(savedResource.getIdentifier())
            .withCreatedDate(savedResource.getCreatedDate())
            .build();
    }

    private ResourceService resourceServiceProvidingDuplicateIdentifiers() {
        Supplier<SortableIdentifier> duplicateIdSupplier = () -> SOME_IDENTIFIER;
        return new ResourceService(client, clock, duplicateIdSupplier);
    }

    private FileSet emptyFileSet() {
        return new FileSet.Builder().build();
    }

    private void assertThatJsonProcessingErrorIsPropagatedUp(Class<JsonProcessingException> expectedExceptionClass,
                                                             Executable action) {
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(expectedExceptionClass)));
    }

    private Set<Publication> createSamplePublications() {

        return
            Set.of(publicationWithIdentifier(), publicationWithIdentifier(), publicationWithIdentifier())
                .stream()
                .map(attempt(res -> resourceService.createPublication(res)))
                .map(Try::orElseThrow)
                .collect(Collectors.toSet());
    }

    private Publication expectedUpdatedResource(Publication sampleResource) {
        return sampleResource.copy()
            .withOwner(someOtherUser().getUserIdentifier())
            .withPublisher(userOrganization(someOtherUser()))
            .withCreatedDate(RESOURCE_CREATION_TIME)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .build();
    }

    private Publication createSampleResource() throws ConflictException {
        var originalResource = publicationWithIdentifier();
        originalResource.setDoi(SOME_DOI);
        return resourceService.createPublication(originalResource);
    }

    private void assertThatUpdateFails(Publication resourceUpdate) {
        Executable action = () -> resourceService.updatePublication(resourceUpdate);
        TransactionCanceledException exception = assertThrows(TransactionCanceledException.class, action);
        String message = exception.getMessage();
        assertThat(message, containsString(TransactionCanceledException.class.getSimpleName()));
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
        Diff diff = javers.compare(oldEntityDescription, newEntityDescription);
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
        return new UserInstance(SOME_OTHER_USER, SOME_OTHER_ORG);
    }

    private Organization anotherPublisher() {
        return new Organization.Builder().withId(SOME_OTHER_ORG).build();
    }

    private Organization newOrganization(URI customerId) {
        return new Organization.Builder().withId(customerId).build();
    }
}