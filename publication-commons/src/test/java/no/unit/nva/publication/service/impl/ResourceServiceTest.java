package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationGenerator.publicationWithIdentifier;
import static no.unit.nva.publication.PublicationGenerator.publicationWithoutIdentifier;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_FILE_SET_FIELD;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_LINK_FIELD;
import static no.unit.nva.publication.service.impl.ResourceService.RESOURCE_WITHOUT_MAIN_TITLE_ERROR;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
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
        String nonExistingIdentifier = SortableIdentifier.next().toString();
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

        UserInstance oldOwner = new UserInstance(sampleResource.getOwner(), sampleResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier().toString(), oldOwner, newOwner);

        assertThatResourceDoesNotExist(sampleResource);

        Publication newResource = resourceService.getPublication(newOwner, sampleResource.getIdentifier());

        Publication expectedResource = expectedUpdatedResource(sampleResource);

        assertThat(newResource, is(equalTo(expectedResource)));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedThenBothOrganizationAndUserAreUpdated()
        throws ApiGatewayException {
        Publication originalResource = createSampleResource();
        UserInstance oldOwner = new UserInstance(originalResource.getOwner(), originalResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(originalResource.getIdentifier().toString(), oldOwner, newOwner);

        Publication newResource = resourceService.getPublication(newOwner, originalResource.getIdentifier());

        assertThat(newResource.getOwner(), is(equalTo(newOwner.getUserIdentifier())));
        assertThat(newResource.getPublisher().getId(), is(equalTo(newOwner.getOrganizationUri())));
    }

    @Test
    public void whenPublicationOwnerIsUpdatedTheModifiedDateIsUpdated()
        throws ApiGatewayException {
        Publication sampleResource = createSampleResource();
        UserInstance oldOwner = new UserInstance(sampleResource.getOwner(), sampleResource.getPublisher().getId());
        UserInstance newOwner = someOtherUser();

        resourceService.updateOwner(sampleResource.getIdentifier().toString(), oldOwner, newOwner);

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
        throws NotFoundException, InvalidPublicationException, ConflictException {
        Publication resource = createSampleResource();
        Publication resourceInResponse = resourceService.publishPublication(resource);
        Publication actualResource = resourceService.getPublication(resource);

        Publication expectedResource = resource.copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .build();

        assertThat(resourceInResponse, is(equalTo(expectedResource)));
        assertThat(actualResource, is(equalTo(expectedResource)));
    }

    @Test
    public void publishResourceReturnsUpdatedResource()
        throws NotFoundException, InvalidPublicationException, ConflictException {
        Publication resource = createSampleResource();
        Publication resourceUpdate = resourceService.publishPublication(resource);

        Publication expectedResource = resource.copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withModifiedDate(RESOURCE_MODIFICATION_TIME)
            .withPublishedDate(RESOURCE_MODIFICATION_TIME)
            .build();

        assertThat(resourceUpdate, is(equalTo(expectedResource)));
    }

    @Test
    public void publishPublicationThrowsConditionalCheckExceptionOnPublishedPublication()
        throws NotFoundException, InvalidPublicationException, ConflictException {
        Publication resource = createSampleResource();

        resourceService.publishPublication(resource);
        assertThrows(ConditionalCheckFailedException.class, () -> resourceService.publishPublication(resource));
    }

    @Test
    public void publishPublicationSetsPublishedDate()
        throws NotFoundException, InvalidPublicationException, ConflictException {
        Publication resource = createSampleResource();
        Publication updatedResource = resourceService.publishPublication(resource);
        assertThat(updatedResource.getPublishedDate(), is(equalTo(RESOURCE_MODIFICATION_TIME)));
    }

    @Test
    public void publishResourceThrowsInvalidPublicationExceptionExceptionWhenResourceHasNoTitle()
        throws ConflictException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.getEntityDescription().setMainTitle(null);
        Publication savedResource = resourceService.createPublication(sampleResource);

        Executable action = () -> resourceService.publishPublication(savedResource);

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

        Executable action = () -> resourceService.publishPublication(savedResource);
        InvalidPublicationException exception = assertThrows(InvalidPublicationException.class, action);
        String actualMessage = exception.getMessage();

        assertThat(actualMessage, containsString(InvalidPublicationException.ERROR_MESSAGE_TEMPLATE));
        assertThat(actualMessage, containsString(sampleResource.getClass()
            .getDeclaredField(RESOURCE_LINK_FIELD).getName()));
        assertThat(actualMessage, containsString(sampleResource.getClass()
            .getDeclaredField(RESOURCE_FILE_SET_FIELD).getName()));
    }

    @Test
    public void publishResourcePublishesResourceWhenLinkIsPresentButNoFiles()
        throws ConflictException, InvalidPublicationException, NotFoundException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setLink(SOME_LINK);
        sampleResource.setFileSet(emptyFileSet());
        Publication savedResource = resourceService.createPublication(sampleResource);
        Publication updatedResource = resourceService.publishPublication(savedResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void publishResourcePublishesResourceWhenResourceHasFilesButNoLink()
        throws ConflictException, InvalidPublicationException, NotFoundException {
        Publication sampleResource = createSampleResource();
        sampleResource.setLink(null);

        Publication updatedResource = resourceService.publishPublication(sampleResource);
        assertThat(updatedResource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    public void publishResourcePublishesShouldThrowExceptionWhenNoReturnValueIsReturned()
        throws JsonProcessingException {
        Publication sampleResource = publicationWithIdentifier();
        sampleResource.setIdentifier(SortableIdentifier.next());

        ResourceService resourceServiceThatReceivesNoValue = resourceServiceReceivingNoValue(sampleResource);

        Executable action = () -> resourceServiceThatReceivesNoValue.publishPublication(sampleResource);
        RuntimeException thrownException = assertThrows(RuntimeException.class, action);

        assertThatCauseIsEmptyValueMapException(thrownException);
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
    public void deleteResourceThrowsExceptionWhenDeletingPublishedPublication()
        throws ApiGatewayException {
        Publication resource = createSampleResource();
        resourceService.publishPublication(resource);
        Executable action =
            () -> resourceService.markPublicationForDeletion(extractUserInstance(resource), resource.getIdentifier());
        ResourceCannotBeDeletedException exception = assertThrows(ResourceCannotBeDeletedException.class, action);
        assertThat(exception.getMessage(), containsString(ResourceCannotBeDeletedException.DEFAULT_MESSAGE));
        assertThat(exception.getMessage(), containsString(resource.getIdentifier().toString()));
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

    private UserInstance extractUserInstance(Publication resource) {
        return new UserInstance(resource.getOwner(), resource.getPublisher().getId());
    }

    private ResourceService resourceServiceReceivingNoValue(Publication publication) throws JsonProcessingException {
        String jsonString = objectMapper.writeValueAsString(new ResourceDao(Resource.fromPublication(publication)));
        Map<String, AttributeValue> getItemResultMap = ItemUtils.toAttributeValues(Item.fromJSON(jsonString));
        AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        when(client.getItem(any(GetItemRequest.class)))
            .thenReturn(new GetItemResult().withItem(getItemResultMap));
        when(client.updateItem(any(UpdateItemRequest.class))).thenReturn(new UpdateItemResult()
            .withAttributes(Collections.emptyMap()));
        return new ResourceService(client, clock);
    }

    private void assertThatCauseIsEmptyValueMapException(RuntimeException thrownException) {
        EmptyValueMapException expectedCause = new EmptyValueMapException();
        assertThat(thrownException.getCause().getClass(), is(equalTo(expectedCause.getClass())));
        assertThat(thrownException.getCause().getMessage(), is(equalTo(expectedCause.getMessage())));
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
        return resourceService.createPublication(originalResource);
    }

    private void assertThatUpdateFails(Publication resourceUpdate) {
        Executable action = () -> resourceService.updatePublication(resourceUpdate);
        ConditionalCheckFailedException exception = assertThrows(ConditionalCheckFailedException.class, action);
        String message = exception.getMessage();
        assertThat(message, containsString(ConditionalCheckFailedException.class.getSimpleName()));
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