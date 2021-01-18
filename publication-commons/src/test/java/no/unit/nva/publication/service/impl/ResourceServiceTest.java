package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_FILES_FIELD;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_LINK_FIELD;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_MAIN_TITLE_FIELD;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static nva.commons.core.JsonUtils.objectMapper;
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
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.File;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.exceptions.EmptyValueMapException;
import no.unit.nva.publication.service.impl.exceptions.ResourceCannotBeDeletedException;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class ResourceServiceTest extends ResourcesDynamoDbLocalTest {

    public static final String ANOTHER_OWNER = "another@owner.no";
    public static final String SOME_OTHER_USER = "some_other@user.no";
    public static final String ORIGINAL_TITLE = "OriginalTitle";
    public static final String UPDATED_TITLE = "UpdatedTitle";
    public static final String SOME_INVALID_FIELD = "someInvalidField";
    public static final String SOME_STRING = "someValue";
    public static final SortableIdentifier SOME_IDENTIFIER = SortableIdentifier.next();
    private static final String SOME_USER = "some@user.com";
    private static final URI SOME_ORG = URI.create("https://example.org/123-456");
    public static final UserInstance SAMPLE_USER = new UserInstance(SOME_USER, SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
    private static final Instant RESOURCE_CREATION_TIME = Instant.parse("1900-12-03T10:15:30.00Z");
    private static final Instant RESOURCE_MODIFICATION_TIME = Instant.parse("2000-01-03T00:00:18.00Z");
    private static final Instant RESOURCE_SECOND_MODIFICATION_TIME = Instant.parse("2010-01-03T02:00:25.00Z");
    private static final Instant RESOURCE_THIRD_MODIFICATION_TIME = Instant.parse("2020-01-03T06:00:32.00Z");
    private static final URI SOME_LINK = URI.create("http://www.example.com/someLink");
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
        Resource resource = sampleResource();
        Resource savedResource = resourceService.createResource(resource);
        Resource readResource = resourceService.getResource(savedResource);
        Resource expectedResource = expectedResourceFromSampleResource(resource, savedResource);
        boolean x = savedResource.equals(expectedResource);
        assertThat(x, is(true));

        assertThat(savedResource, is(equalTo(expectedResource)));
        assertThat(readResource, is(equalTo(expectedResource)));
        assertThat(readResource, is(not(sameInstance(expectedResource))));
    }

    @Test
    public void createResourceThrowsConflictExceptionWhenResourceWithSameIdentifierExists() throws ConflictException {
        final Resource sampleResource = createSampleResource();
        final Resource collidingResource = sampleResource.copy()
            .withPublisher(anotherPublisher())
            .withOwner(ANOTHER_OWNER)
            .build();
        ResourceService resourceService = resourceServiceProvidingDuplicateIdentifiers();
        resourceService.createResource(sampleResource);
        Executable action = () -> resourceService.createResource(collidingResource);
        assertThrows(ConflictException.class, action);

        assertThat(sampleResource.getIdentifier(), is(equalTo(collidingResource.getIdentifier())));
        assertThat(sampleResource.getOwner(), is(not(equalTo(collidingResource.getOwner()))));
        assertThat(sampleResource.getPublisher().getId(), is(not(equalTo(collidingResource.getPublisher().getId()))));
    }

    @Test
    public void createResourceSavesResourcesWithSameOwnerAndPublisherButDifferentIdentifier()
        throws ConflictException {
        final Resource sampleResource = sampleResource();
        final Resource anotherResource = sampleResource();

        resourceService.createResource(sampleResource);
        assertDoesNotThrow(() -> resourceService.createResource(anotherResource));
    }

    @Test
    public void getResourceByIdentifierReturnsNotFoundWhenResourceDoesNotExist() {
        String nonExistingIdentifier = SortableIdentifier.next().toString();
        Executable action = () -> resourceService.getResource(SAMPLE_USER, nonExistingIdentifier);
        assertThrows(NotFoundException.class, action);
    }

    @Test
    public void getResourceByIdentifierReturnsResourceWhenResourceExists() throws NotFoundException, ConflictException {
        Resource sampleResource = createSampleResource();
        Resource savedResource = resourceService.getResource(SAMPLE_USER, sampleResource.getIdentifier());
        assertThat(savedResource, is(equalTo(sampleResource)));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedTheResourceEntryMaintainsTheRestResourceMetadata()
        throws ConflictException, NotFoundException {
        Resource sampleResource = createSampleResource();

        UserInstance oldOwner = new UserInstance(sampleResource.getOwner(), sampleResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier().toString(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Resource newResource = resourceService.getResource(newOwner, sampleResource.getIdentifier());

        Resource expectedResource = expectedUpdatedResource(sampleResource);

        assertThat(newResource, is(equalTo(expectedResource)));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedThenBothOrganizationAndUserAreUpdated()
        throws ConflictException, NotFoundException {
        Resource originalResource = createSampleResource();
        UserInstance oldOwner = new UserInstance(originalResource.getOwner(), originalResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier().toString(), oldOwner, newOwner);

        Resource newResource = resourceService.getResource(newOwner, originalResource.getIdentifier());

        assertThat(newResource.getOwner(), is(equalTo(newOwner.getUserIdentifier())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getOrganizationUri())));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedTheModifiedDateIsUpdated()
        throws ConflictException, NotFoundException {
        Resource sampleResource = createSampleResource();
        UserInstance oldOwner = new UserInstance(sampleResource.getOwner(), sampleResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier().toString(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Resource newResource = resourceService.getResource(newOwner, sampleResource.getIdentifier());

        assertThat(newResource.getModifiedDate(), is(equalTo(RESOURCE_MODIFICATION_TIME)));
    }

    @Test
    public void resourceIsUpdatedWhenResourceUpdateIsReceived() throws ConflictException, NotFoundException {
        Resource resource = createSampleResource();
        Resource actualOriginalResourcce = resourceService.getResource(resource);
        assertThat(actualOriginalResourcce, is(equalTo(resource)));

        Resource resourceUpdate = createResourceUpdate(resource);

        resourceService.updateResource(resourceUpdate);
        Resource actualUpdatedResource = resourceService.getResource(resource);

        assertThat(actualUpdatedResource, is(equalTo(resourceUpdate)));
        assertThat(actualUpdatedResource, is(not(equalTo(actualOriginalResourcce))));
    }

    @Test
    @DisplayName("resourceUpdate fails when Update changes the primary key (owner-part)")
    public void resourceUpdateFailsWhenUpdateChangesTheOwnerPartOfThePrimaryKey() throws ConflictException {
        Resource resource = createSampleResource();
        Resource resourceUpdate = createResourceUpdate(resource);

        resourceUpdate.setOwner(ANOTHER_OWNER);
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    @DisplayName("resourceUpdate fails when Update changes the primary key (organization-part)")
    public void resourceUpdateFailsWhenUpdateChangesTheOrganizationPartOfThePrimaryKey()
        throws ConflictException {
        Resource resource = createSampleResource();
        Resource resourceUpdate = createResourceUpdate(resource);

        resourceUpdate.setPublisher(newOrganization(SOME_OTHER_ORG));
        assertThatUpdateFails(resourceUpdate);
    }

    @Test
    @DisplayName("resourceUpdate fails when Update changes the primary key (primary-key-part)")
    public void resourceUpdateFailsWhenUpdateChangesTheIdentifierPartOfThePrimaryKey()
        throws ConflictException {
        Resource resource = createSampleResource();
        Resource resourceUpdate = createResourceUpdate(resource);

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

        Resource resource = sampleResource();
        Executable action = () -> failingService.createResource(resource);
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
        Resource resource = sampleResource();

        ResourceService failingResourceService = new ResourceService(client, clock);

        Executable action = () -> failingResourceService.getResource(resource);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), is(equalTo(expectedMessage)));
    }

    @Test
    public void getResourcesByOwnerReturnsAllResourcesOwnedByUser()  {
        Set<Resource> userResources = createSampleResources();

        List<Resource> actualResources = resourceService.getResourcesByOwner(SAMPLE_USER);
        HashSet<Resource> actualResourcesSet = new HashSet<>(actualResources);

        assertThat(actualResourcesSet, is(equalTo(userResources)));
    }

    @Test
    public void getResourcesByOwnerReturnsEmptyListWhenUseHasNoPublications() {

        List<Resource> actualResources = resourceService.getResourcesByOwner(SAMPLE_USER);
        HashSet<Resource> actualResourcesSet = new HashSet<>(actualResources);

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
        Executable action = () -> failingResourceService.getResource(SAMPLE_USER, someIdentifier);

        assertThatJsonProcessingErrorIsPropagatedUp(expectedExceptionClass, action);
    }

    @Test
    public void publishResourceSetsPublicationStatusToPublished()
        throws ConflictException, NotFoundException, JsonProcessingException, InvalidPublicationException {
        Resource resource = createSampleResource();
        Resource resourceInResponse = resourceService.publishResource(resource);
        Resource actualResource = resourceService.getResource(resource);

        Resource expectedResource = resource.copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .build();

        assertThat(resourceInResponse, is(equalTo(expectedResource)));
        assertThat(actualResource, is(equalTo(expectedResource)));
    }

    @Test
    public void publishResourceReturnsUpdatedResource()
        throws ConflictException, NotFoundException, JsonProcessingException, InvalidPublicationException {
        Resource resource = createSampleResource();
        Resource resourceUpdate = resourceService.publishResource(resource);

        Resource expectedResource = resource.copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .build();

        assertThat(resourceUpdate, is(equalTo(expectedResource)));
    }

    @Test
    public void publishPublicationHasNoEffectOnAlreadyPublishedResource()
        throws ConflictException, NotFoundException, JsonProcessingException, InvalidPublicationException {
        Resource resource = createSampleResource();
        resourceService.publishResource(resource);
        Resource updatedResource = resourceService.publishResource(resource);
        Resource expectedResource = resource.copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .build();

        assertThat(updatedResource, is(equalTo(expectedResource)));
    }

    @Test
    public void publishPublicationSetsPublishedDate()
        throws ConflictException, NotFoundException, JsonProcessingException, InvalidPublicationException {
        Resource resource = createSampleResource();
        Resource updatedResource = resourceService.publishResource(resource);
        assertThat(updatedResource.getPublishedDate(), is(equalTo(RESOURCE_MODIFICATION_TIME)));
    }

    @Test
    public void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoTitle()
        throws ConflictException {
        Resource sampleResource = sampleResource();
        sampleResource.setTitle(null);
        Resource savedResource = resourceService.createResource(sampleResource);

        Executable action = () -> resourceService.publishResource(savedResource);

        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, containsString(InvalidPublicationException.ERROR_MESSAGE_TEMPLATE));
        assertThat(actualMessage, containsString(RESOURCE_MAIN_TITLE_FIELD));
    }

    @Test
    public void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoLinkAndNoFiles()
        throws ConflictException, NoSuchFieldException {
        Resource sampleResource = sampleResource();
        sampleResource.setLink(null);
        sampleResource.setFiles(emptyFileSet());
        Resource savedResource = resourceService.createResource(sampleResource);

        Executable action = () -> resourceService.publishResource(savedResource);
        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();

        assertThat(actualMessage, containsString(InvalidPublicationException.ERROR_MESSAGE_TEMPLATE));
        assertThat(actualMessage, containsString(sampleResource.getClass()
            .getDeclaredField(RESOURCE_LINK_FIELD).getName()));
        assertThat(actualMessage, containsString(sampleResource.getClass()
            .getDeclaredField(RESOURCE_FILES_FIELD).getName()));
    }

    @Test
    public void publishResourcePublishesResourceWhenLinkIsPresentButNoFiles()
        throws ConflictException, InvalidPublicationException, NotFoundException,
               JsonProcessingException {
        Resource sampleResource = sampleResource();
        sampleResource.setFiles(emptyFileSet());
        Resource savedResource = resourceService.createResource(sampleResource);
        Resource updatedResource = resourceService.publishResource(savedResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void publishResourcePublishesResourceWhenResourceHasFilesButNoLink()
        throws ConflictException, InvalidPublicationException, NotFoundException,
               JsonProcessingException {
        Resource sampleResource = createSampleResource();
        sampleResource.setLink(null);

        Resource updatedResource = resourceService.publishResource(sampleResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void publishResourcePublishesShouldThrowExceptionWhenNoReturnValueIsReturned()
        throws JsonProcessingException {
        Resource sampleResource = sampleResource();
        sampleResource.setIdentifier(SortableIdentifier.next());

        ResourceService resourceServiceThatReceivesNoValue = resourceServiceReceivingNoValue(sampleResource);

        Executable action = () -> resourceServiceThatReceivesNoValue.publishResource(sampleResource);
        RuntimeException thrownException = assertThrows(RuntimeException.class, action);

        assertThatCauseIsEmptyValueMapException(thrownException);
    }

    private void assertThatCauseIsEmptyValueMapException(RuntimeException thrownException) {
        EmptyValueMapException expectedCause = new EmptyValueMapException();
        assertThat(thrownException.getCause().getClass(), is(equalTo(expectedCause.getClass())));
        assertThat(thrownException.getCause().getMessage(), is(equalTo(expectedCause.getMessage())));
    }

    private ResourceService resourceServiceReceivingNoValue(Resource resource) throws JsonProcessingException {
        String jsonString = objectMapper.writeValueAsString(new ResourceDao(resource));
        Map<String, AttributeValue> getItemResultMap = ItemUtils.toAttributeValues(Item.fromJSON(jsonString));
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        when(client.getItem(any(GetItemRequest.class)))
            .thenReturn(new GetItemResult().withItem(getItemResultMap));
        when(client.updateItem(any(UpdateItemRequest.class))).thenReturn(new UpdateItemResult()
            .withAttributes(Collections.emptyMap()));
        return new ResourceService(client, clock);
    }

    @Test
    public void createResourceReturnsNewIdentifierWhenResourceIsCreated() throws ConflictException {
        Resource sampleResource = sampleResource();
        Resource savedResource = resourceService.createResource(sampleResource);
        assertThat(sampleResource.getIdentifier(), is(equalTo(null)));
        assertThat(savedResource.getIdentifier(), is(notNullValue()));
    }

    @Test
    public void deletePublicationCanMarkDraftForDeletion() throws ApiGatewayException, JsonProcessingException {
        Resource resource = createSampleResource();

        Resource resourceUpdate = resourceService.markPublicationForDeletion(resource);
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        Resource resourceForDeletion = resourceService.getResource(resource);
        assertThat(resourceForDeletion.getStatus(),
            Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    public void deletePublicationReturnsUpdatedResourceCanMarkDraftForDeletion()
        throws ApiGatewayException, JsonProcessingException {
        Resource resource = createSampleResource();

        Resource resourceUpdate = resourceService.markPublicationForDeletion(resource);
        assertThat(resourceUpdate.getStatus(), Matchers.is(Matchers.equalTo(PublicationStatus.DRAFT_FOR_DELETION)));
    }

    @Test
    public void deleteResourceThrowsExceptionWhenDeletingPublishedPublication()
        throws ApiGatewayException, JsonProcessingException {
        Resource resource = createSampleResource();
        resourceService.publishResource(resource);
        Executable action = () -> resourceService.markPublicationForDeletion(resource);
        ResourceCannotBeDeletedException exception = assertThrows(ResourceCannotBeDeletedException.class, action);
        assertThat(exception.getMessage(), containsString(ResourceCannotBeDeletedException.DEFAULT_MESSAGE));
        assertThat(exception.getMessage(), containsString(resource.getIdentifier().toString()));
    }

    @Test
    public void deleteResourceThrowsNoErrorWhenDeletingPublicationThatIsMarkedForDeletion()
        throws ApiGatewayException, JsonProcessingException {
        Resource resource = createSampleResource();
        resourceService.markPublicationForDeletion(resource);
        Resource actualResource = resourceService.getResource(resource);
        assertThat(actualResource.getStatus(), is(equalTo(PublicationStatus.DRAFT_FOR_DELETION)));

        assertDoesNotThrow(() -> resourceService.markPublicationForDeletion(resource));
    }

    private Resource expectedResourceFromSampleResource(Resource sampleResource, Resource savedResource) {
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

    private Set<Resource> createSampleResources() {

        return
            Set.of(sampleResource(), sampleResource(), sampleResource()).stream()
                .map(attempt(res -> resourceService.createResource(res)))
                .map(Try::orElseThrow)
                .collect(Collectors.toSet());
    }

    private Resource expectedUpdatedResource(Resource sampleResource) {
        return sampleResource.copy()
            .withOwner(someOtherUser().getUserIdentifier())
            .withPublisher(userOrganization(someOtherUser()))
            .withCreatedDate(RESOURCE_CREATION_TIME)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .build();
    }

    private Resource createSampleResource() throws ConflictException {
        Resource originalResource = sampleResource();
        return resourceService.createResource(originalResource);
    }

    private void assertThatUpdateFails(Resource resourceUpdate) {
        Executable action = () -> resourceService.updateResource(resourceUpdate);
        ConditionalCheckFailedException exception = assertThrows(ConditionalCheckFailedException.class, action);
        String message = exception.getMessage();
        assertThat(message, containsString(ConditionalCheckFailedException.class.getSimpleName()));
    }

    private Resource createResourceUpdate(Resource resource) {
        return resource.copy().withTitle(UPDATED_TITLE).build();
    }

    private void assertThatResourceDoesNotExist(Resource sampleResource) {
        assertThrows(NotFoundException.class, () -> resourceService.getResource(sampleResource));
    }

    private UserInstance someOtherUser() {
        return new UserInstance(SOME_OTHER_USER, SOME_OTHER_ORG);
    }

    private Organization anotherPublisher() {
        return new Organization.Builder().withId(SOME_OTHER_ORG).build();
    }

    private Resource sampleResource() {

        FileSet files = new FileSet();
        File file = new File.Builder().withIdentifier(UUID.randomUUID()).build();
        files.setFiles(List.of(file));
        return Resource.builder()
            .withTitle(ORIGINAL_TITLE)
            .withStatus(PublicationStatus.DRAFT)
            .withOwner(SOME_USER)
            .withPublisher(newOrganization(SOME_ORG))
            .withFiles(files)
            .withLink(SOME_LINK)
            .build();
    }

    private Organization newOrganization(URI customerId) {
        return new Organization.Builder().withId(customerId).build();
    }
}