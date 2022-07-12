package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.Dao.CONTAINED_DATA_FIELD_NAME;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.PublishingRequestDao;
import no.unit.nva.publication.storage.model.daos.UniquePublishingRequestEntry;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class PublishingRequestService extends ServiceWithTransactions {

    public static final String PUBLISHING_REQUEST_NOT_FOUND_FOR_RESOURCE =
        "Could not find a Publishing Request for Resource: ";
    public static final String ALREADY_PUBLISHED_ERROR =
        "Publication is already published.";
    public static final String MARKED_FOR_DELETION_ERROR =
        "Publication is marked for deletion and cannot be published.";
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;
    private final AmazonDynamoDB client;
    private final Clock clock;
    private final String tableName;
    private final Supplier<SortableIdentifier> identifierProvider;
    private final ResourceService resourceService;

    public PublishingRequestService(AmazonDynamoDB client, Clock clock) {
        this(client, clock, DEFAULT_IDENTIFIER_PROVIDER);
    }

    protected PublishingRequestService(AmazonDynamoDB client,
                                       Clock clock,
                                       Supplier<SortableIdentifier> identifierProvider) {
        super();
        this.client = client;
        this.clock = clock;
        this.tableName = RESOURCES_TABLE_NAME;
        this.identifierProvider = identifierProvider;
        resourceService = new ResourceService(client, clock, identifierProvider);
    }

    public PublishingRequestCase createPublishingRequest(PublishingRequestCase publishingRequest)
        throws ApiGatewayException {
        var associatePublication = fetchPublication(publishingRequest);
        return fromPublication(associatePublication);
    }

    public PublishingRequestCase getPublishingRequest(PublishingRequestCase queryObject)
        throws NotFoundException {

        var queryResult = getFromDatabase(queryObject);
        return attempt(queryResult::getItem)
            .map(item -> parseAttributeValuesMap(item, PublishingRequestDao.class))
            .map(PublishingRequestDao::getData)
            .toOptional()
            .orElseThrow(() -> handleFetchPublishingRequestByResourceError(queryObject.getIdentifier()));
    }

    public void updatePublishingRequest(PublishingRequestCase requestUpdate) {
        var entryUpdate = requestUpdate.copy();
        entryUpdate.setModifiedDate(clock.instant());
        entryUpdate.setRowVersion(UUID.randomUUID().toString());
        var putItemRequest = cratePutItemRequest(entryUpdate);
        client.putItem(putItemRequest);
    }

    private PutItemRequest cratePutItemRequest(PublishingRequestCase entryUpdate) {
        var dao = new PublishingRequestDao(entryUpdate);

        final var expressionAttributeNames = Map.of(
            "#data", CONTAINED_DATA_FIELD_NAME,
            "#status", PublishingRequestCase.STATUS_FIELD
        );
        final var expressionAttributeValues = Map.of(
            ":status", new AttributeValue(dao.getData().getStatus().name())
        );
        return new PutItemRequest()
            .withTableName(tableName)
            .withItem(dao.toDynamoFormat())
            .withConditionExpression("#data.#status <> :status")
            .withExpressionAttributeNames(expressionAttributeNames)
            .withExpressionAttributeValues(expressionAttributeValues);
    }

    public PublishingRequestCase getPublishingRequestByPublicationAndRequestIdentifiers(
        SortableIdentifier publicationIdentifier,
        SortableIdentifier publishingRequestIdentifier)
        throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        var userInstance = UserInstance.fromPublication(publication);
        var queryObject = PublishingRequestCase.createQuery(userInstance,
                                                            publicationIdentifier,
                                                            publishingRequestIdentifier);
        return getPublishingRequest(queryObject);
    }

    @Override
    protected String getTableName() {
        return tableName;
    }

    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }

    @Override
    @JacocoGenerated
    protected Clock getClock() {
        return clock;
    }

    private static NotFoundException handleFetchPublishingRequestByResourceError(
        SortableIdentifier resourceIdentifier) {
        return new NotFoundException(PUBLISHING_REQUEST_NOT_FOUND_FOR_RESOURCE + resourceIdentifier.toString());
    }

    private Publication fetchPublication(PublishingRequestCase publishingRequest)
        throws ApiGatewayException {
        var userInstance = UserInstance.create(publishingRequest.getOwner(), publishingRequest.getCustomerId());
        return resourceService.getPublication(userInstance, publishingRequest.getResourceIdentifier());
    }

    private PublishingRequestCase fromPublication(Publication publication)
        throws ConflictException {
        verifyPublicationIsPublishable(publication);
        var publishingRequest = createNewPublishingRequestEntry(publication);
        var request = createInsertionTransactionRequest(publishingRequest);
        sendTransactionWriteRequest(request);
        return publishingRequest;
    }

    private GetItemResult getFromDatabase(PublishingRequestCase queryObject) {
        var queryDao = PublishingRequestDao.queryObject(queryObject);
        var getItemRequest = new GetItemRequest()
            .withTableName(tableName)
            .withKey(queryDao.primaryKey());
        return client.getItem(getItemRequest);
    }

    private void verifyPublicationIsPublishable(Publication publication) throws ConflictException {
        if (PublicationStatus.PUBLISHED == publication.getStatus()) {
            throw new ConflictException(ALREADY_PUBLISHED_ERROR);
        }
        if (PublicationStatus.DRAFT_FOR_DELETION == publication.getStatus()) {
            throw new ConflictException(MARKED_FOR_DELETION_ERROR);
        }
    }

    private PublishingRequestCase createNewPublishingRequestEntry(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        var entry = PublishingRequestCase.createOpeningCaseObject(userInstance, publication.getIdentifier());
        entry.setCreatedDate(clock.instant());
        entry.setIdentifier(identifierProvider.get());
        entry.setRowVersion(UUID.randomUUID().toString());
        return entry;
    }

    private TransactWriteItemsRequest createInsertionTransactionRequest(PublishingRequestCase publishingRequest) {
        var publicationRequestEntry = createPublishingRequestInsertionEntry(publishingRequest);
        var identifierEntry = createUniqueIdentifierEntry(publishingRequest);
        var publishingRequestUniquenessEntry = createPublishingRequestUniquenessEntry(publishingRequest);
        return new TransactWriteItemsRequest()
            .withTransactItems(
                identifierEntry,
                publicationRequestEntry,
                publishingRequestUniquenessEntry);
    }

    private TransactWriteItem createPublishingRequestUniquenessEntry(PublishingRequestCase publishingRequest) {
        var publishingRequestUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        return newPutTransactionItem(publishingRequestUniquenessEntry);
    }

    private TransactWriteItem createPublishingRequestInsertionEntry(PublishingRequestCase publicationRequest) {
        var dynamoEntry = new PublishingRequestDao(publicationRequest);
        return newPutTransactionItem(dynamoEntry);
    }

    private TransactWriteItem createUniqueIdentifierEntry(PublishingRequestCase publicationRequest) {
        var identifierEntry = new IdentifierEntry(publicationRequest.getIdentifier().toString());
        return newPutTransactionItem(identifierEntry);
    }
}
