package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationGenerator.publicationWithoutIdentifier;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static nva.commons.utils.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.exceptions.commonexceptions.ConflictException;
import nva.commons.exceptions.commonexceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class ResourceServiceTest extends ResourcesDynamoDbLocalTest {

    public static final String ANOTHER_OWNER = "another@owner.no";
    public static final String SOME_OTHER_USER = "some_other@user.no";
    public static final String ORIGINAL_TITLE = "OriginalTitle";
    public static final String UPDATED_TITLE = "UpdatedTitle";
    private static final String SOME_USER = "some@user.com";
    private static final URI SOME_ORG = URI.create("https://example.org/123-456");
    public static final UserInstance SAMPLE_USER = new UserInstance(SOME_USER, SOME_ORG);
    private static final URI SOME_OTHER_ORG = URI.create("https://example.org/789-ABC");
    private static final Instant RESOURCE_CREATION_TIME = Instant.parse("1900-12-03T10:15:30.00Z");
    private static final Instant RESOURCE_MODIFIED_TIME = Instant.parse("2000-01-03T00:00:18.00Z");
    public static final String SOME_INVALID_FIELD = "someInvalidField";
    public static final String SOME_STRING = "someValue";
    private ResourceService resourceService;
    private Clock clock;

    @BeforeEach
    public void init() {
        super.init();
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(RESOURCE_CREATION_TIME).thenReturn(RESOURCE_MODIFIED_TIME);
        resourceService = new ResourceService(client, clock);
    }

    @Test
    public void createResourceCreatesResource() throws NotFoundException, ConflictException {
        Resource resource = Resource.fromPublication(publicationWithoutIdentifier());
        resourceService.createResource(resource);
        Resource savedResource = resourceService.getResource(resource);
        assertThat(savedResource, is(equalTo(resource)));
        assertThat(savedResource, is(not(sameInstance(resource))));
    }

    @Test
    public void createResourceThrowsConflictExceptionWhenResourceWithSameIdentifierExists() throws ConflictException {
        final Resource sampleResource = emptyResource();
        final Resource collidingResource = sampleResource.copy()
            .withPublisher(anotherPublisher())
            .withOwner(ANOTHER_OWNER)
            .build();

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
        final Resource sampleResource = emptyResource();
        final Resource anotherResource = emptyResource();

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
        Resource sampleResource = emptyResource();
        UserInstance oldOwner = new UserInstance(sampleResource.getOwner(), sampleResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.createResource(sampleResource);
        resourceService.updateOwner(sampleResource.getIdentifier().toString(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Resource newResource = resourceService.getResource(newOwner, sampleResource.getIdentifier());

        assertThat(newResource.getModifiedDate(), is(equalTo(RESOURCE_MODIFIED_TIME)));
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

        Resource resource = emptyResource();
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
        Resource resource = emptyResource();

        ResourceService failingResourceService = new ResourceService(client, clock);

        Executable action = () -> failingResourceService.getResource(resource);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), is(equalTo(expectedMessage)));
    }

    @Test
    public void getResourcesByOwnerReturnsAllResourcesOwnedByUser() throws ConflictException {
        Set<Resource> userResources = createSampleResources();

        List<Resource> actualResources = resourceService.getResourcesByOwner(SAMPLE_USER);
        HashSet<Resource> actualResourcesSet = new HashSet<>(actualResources);

        assertThat(actualResourcesSet, is(equalTo(userResources)));
    }

    @Test
    public void getResourcesByOwnerReturnsEmpyListWhenUseHasNoPublications() {

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

    private void assertThatJsonProcessingErrorIsPropagatedUp(Class<JsonProcessingException> expectedExceptionClass,
                                                             Executable action) {
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(expectedExceptionClass)));
    }

    private Set<Resource> createSampleResources() throws ConflictException {
        Set<Resource> userResources = Set.of(emptyResource(), emptyResource(), emptyResource());
        for (Resource resource : userResources) {
            resourceService.createResource(resource);
        }
        return userResources;
    }

    private Resource expectedUpdatedResource(Resource sampleResource) {
        return sampleResource.copy()
            .withOwner(someOtherUser().getUserIdentifier())
            .withPublisher(userOrganization(someOtherUser()))
            .withCreatedDate(RESOURCE_CREATION_TIME)
            .withModifiedDate(RESOURCE_MODIFIED_TIME)
            .build();
    }

    private Resource createSampleResource() throws ConflictException {
        Resource originalResource = emptyResource();
        resourceService.createResource(originalResource);
        return originalResource;
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

    private Resource emptyResource() {
        Resource resource = Resource.emptyResource(SAMPLE_USER.getUserIdentifier(), SAMPLE_USER.getOrganizationUri());
        resource.setStatus(PublicationStatus.DRAFT);
        resource.setTitle(ORIGINAL_TITLE);
        return resource;
    }

    private Organization newOrganization(URI customerId) {
        return new Organization.Builder().withId(customerId).build();
    }
}