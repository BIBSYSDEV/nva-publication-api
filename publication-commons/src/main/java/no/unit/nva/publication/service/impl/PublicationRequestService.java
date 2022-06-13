package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.storage.model.PublicationRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.PublicationRequestDao;
import no.unit.nva.publication.storage.model.daos.UniquePublicationRequestEntry;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;

import java.time.Clock;
import java.util.Map;
import java.util.function.Supplier;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;

public class PublicationRequestService extends ServiceWithTransactions {

    public static final String PUBLICATION_REQUEST_NOT_FOUND_FOR_RESOURCE = "Could not find a PublicationRequest for Resource: ";
    public static final String ALREADY_PUBLISHED_OR_DELETED_AND_CANNOT_BE_PUBLISHED = "Publication is already published or deleted and cannot be published";

    private final AmazonDynamoDB client;
    private final Clock clock;
    private final String tableName;
    private final Supplier<SortableIdentifier> identifierProvider;
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;



    public PublicationRequestService(AmazonDynamoDB client,Clock clock) {
        this(client,
                clock,
                DEFAULT_IDENTIFIER_PROVIDER);
    }

    protected PublicationRequestService(AmazonDynamoDB client,
                                Clock clock,
                                Supplier<SortableIdentifier> identifierProvider) {
        super();
        this.client = client;
        this.clock = clock;
        this.tableName = RESOURCES_TABLE_NAME;
        this.identifierProvider = identifierProvider;
    }

    public SortableIdentifier createPublicationRequest(Publication publication) throws BadRequestException, TransactionFailedException {
        verifyPublicationIsPublishable(publication);
        PublicationRequest publicationRequest = createNewPublicationRequestEntry(publication);
        TransactWriteItemsRequest request = createInsertionTransactionRequest(publicationRequest);
        sendTransactionWriteRequest(request);
        return publicationRequest.getIdentifier();
    }

    private void verifyPublicationIsPublishable(Publication publication) throws BadRequestException {
        if (publication.getStatus() == PublicationStatus.PUBLISHED
                || publication.getStatus() == PublicationStatus.DRAFT_FOR_DELETION) {
            throw new BadRequestException(ALREADY_PUBLISHED_OR_DELETED_AND_CANNOT_BE_PUBLISHED);
        }
    }

    public PublicationRequest getPublicationRequestByResourceIdentifier(UserInstance resourceOwner,
                                                        SortableIdentifier resourceIdentifier)
            throws NotFoundException {
        return getPublicationRequestByResourceIdentifier(resourceOwner, resourceIdentifier, tableName, client);
    }

    private PublicationRequest createNewPublicationRequestEntry(Publication publication) {
        Resource resource = Resource.fromPublication(publication);
        return PublicationRequest.newPublicationRequestResource(identifierProvider.get(), resource, clock.instant());
    }

    private TransactWriteItemsRequest createInsertionTransactionRequest(PublicationRequest publicationRequest) {
        TransactWriteItem publicationRequestEntry = createPublicationRequestInsertionEntry(publicationRequest);
        TransactWriteItem identifierEntry = createUniqueIdentifierEntry(publicationRequest);
        TransactWriteItem uniquePublicationRequestEntry = createUniquePublicationRequestEntry(publicationRequest);

        return new TransactWriteItemsRequest()
                .withTransactItems(
                        identifierEntry,
                        uniquePublicationRequestEntry,
                        publicationRequestEntry);
    }

    private TransactWriteItem createPublicationRequestInsertionEntry(PublicationRequest publicationRequest) {
        return newPutTransactionItem(new PublicationRequestDao(publicationRequest));
    }

    private TransactWriteItem createUniquePublicationRequestEntry(PublicationRequest publicationRequest) {
        UniquePublicationRequestEntry uniquePublicationRequestEntry = new UniquePublicationRequestEntry(
                publicationRequest.getResourceIdentifier().toString());
        return newPutTransactionItem(uniquePublicationRequestEntry);
    }

    private TransactWriteItem createUniqueIdentifierEntry(PublicationRequest publicationRequest) {
        IdentifierEntry identifierEntry = new IdentifierEntry(publicationRequest.getIdentifier().toString());
        return newPutTransactionItem(identifierEntry);
    }

    public static PublicationRequest getPublicationRequestByResourceIdentifier(UserInstance resourceOwner,
                                                               SortableIdentifier resourceIdentifier,
                                                               String tableName,
                                                               AmazonDynamoDB client
    ) throws NotFoundException {
        PublicationRequestDao queryObject = PublicationRequestDao.queryByCustomerAndResourceIdentifier(resourceOwner,
                resourceIdentifier);
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                .withKeyConditions(
                        queryObject.byResource(PublicationRequestDao.joinByResourceContainedOrderedType()));
        QueryResult queryResult = client.query(queryRequest);

        Map<String, AttributeValue> item = parseQueryResultExpectingSingleItem(queryResult)
                .orElseThrow(
                        fail -> handleFetchPublicationRequestByResoureError(resourceIdentifier));
        PublicationRequestDao dao = parseAttributeValuesMap(item, PublicationRequestDao.class);
        return dao.getData();
    }

    private static Try<Map<String, AttributeValue>> parseQueryResultExpectingSingleItem(QueryResult queryResult) {
        return attempt(() -> queryResult.getItems()
                .stream()
                .collect(SingletonCollector.collect()));
    }

    private static NotFoundException handleFetchPublicationRequestByResoureError(SortableIdentifier resourceIdentifier) {
        return new NotFoundException(PUBLICATION_REQUEST_NOT_FOUND_FOR_RESOURCE + resourceIdentifier.toString());
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


}
