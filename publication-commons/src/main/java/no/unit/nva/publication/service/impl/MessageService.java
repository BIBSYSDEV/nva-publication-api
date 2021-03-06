package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.InvalidInputException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.MessageDao;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public class MessageService extends ServiceWithTransactions {


    public static final String EMPTY_MESSAGE_ERROR = "Message cannot be empty";

    public static final String MESSAGE_NOT_FOUND_ERROR = "Could not find message with identifier:";

    private final AmazonDynamoDB client;
    private final String tableName;
    private final Clock clockForTimestamps;

    private final Supplier<SortableIdentifier> identifierSupplier;

    public MessageService(AmazonDynamoDB client, Clock clockForTimestamps) {
        this(client, clockForTimestamps, defaultIdentifierSupplier());
    }

    public MessageService(AmazonDynamoDB client,
                          Clock clockForTimestamps,
                          Supplier<SortableIdentifier> identifierSupplier) {
        super();
        this.client = client;
        tableName = RESOURCES_TABLE_NAME;
        this.clockForTimestamps = clockForTimestamps;
        this.identifierSupplier = identifierSupplier;
    }


    public SortableIdentifier createSimpleMessage(UserInstance sender, Publication publication, String messageText)
        throws TransactionFailedException {
        Message message = createNewSimpleMessage(sender, publication, messageText);
        return writeMessageToDb(message);
    }

    public SortableIdentifier createDoiRequestMessage(UserInstance sender, Publication publication, String messageText)
        throws TransactionFailedException {
        Message message = createNewDoiRequestMessage(sender, publication, messageText);
        return writeMessageToDb(message);
    }

    public SortableIdentifier writeMessageToDb(Message message) throws TransactionFailedException {
        TransactWriteItem dataWriteItem = newPutTransactionItem(new MessageDao(message));

        IdentifierEntry identifierEntry = new IdentifierEntry(message.getIdentifier().toString());
        TransactWriteItem identifierWriteItem = newPutTransactionItem(identifierEntry);

        TransactWriteItemsRequest request = newTransactWriteItemsRequest(dataWriteItem, identifierWriteItem);
        sendTransactionWriteRequest(request);
        return message.getIdentifier();
    }

    public Message getMessage(UserInstance owner, URI messageId) throws NotFoundException {
        SortableIdentifier identifier = SortableIdentifier.fromUri(messageId);
        return getMessage(owner, identifier);
    }

    public Message getMessage(UserInstance owner, SortableIdentifier identifier) throws NotFoundException {
        MessageDao queryObject = MessageDao.queryObject(owner, identifier);
        Map<String, AttributeValue> item = fetchMessage(queryObject);
        MessageDao result = parseAttributeValuesMap(item, MessageDao.class);
        return result.getData();
    }

    public Optional<ResourceConversation> getMessagesForResource(UserInstance user, SortableIdentifier identifier) {
        ResourceDao queryObject = ResourceDao.queryObject(user, identifier);
        QueryRequest queryRequest = queryForRetrievingMessagesAndRespectiveResource(queryObject);
        QueryResult queryResult = client.query(queryRequest);
        List<Message> messagesPerResource = parseMessages(queryResult);
        return ResourceConversation.fromMessageList(messagesPerResource).stream().findFirst();
    }

    public List<ResourceConversation> listMessagesForCurator(URI customerId, MessageStatus messageStatus) {
        MessageDao queryObject = MessageDao.listMessagesForCustomerAndStatus(customerId, messageStatus);
        QueryRequest queryRequest = queryRequestForListingMessagesByCustomerAndStatus(queryObject);
        QueryResult queryResult = client.query(queryRequest);
        List<Message> messagesPerResource = parseMessages(queryResult);
        return ResourceConversation.fromMessageList(messagesPerResource);
    }

    public List<ResourceConversation> listMessagesForUser(UserInstance owner) {
        MessageDao queryObject = MessageDao.listMessagesAndResourcesForUser(owner);
        QueryRequest queryRequest = queryForFetchingAllMessagesForAUser(queryObject);
        QueryResult queryResult = client.query(queryRequest);
        List<Message> messagesPerResource = parseMessages(queryResult);
        return ResourceConversation.fromMessageList(messagesPerResource);
    }

    @Override
    protected String getTableName() {
        return tableName;
    }

    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }

    @JacocoGenerated
    @Override
    protected Clock getClock() {
        return clockForTimestamps;
    }

    private static Supplier<SortableIdentifier> defaultIdentifierSupplier() {
        return SortableIdentifier::next;
    }

    private Map<String, AttributeValue> fetchMessage(MessageDao queryObject) throws NotFoundException {

        GetItemRequest getMessageRequest = getMessageByPrimaryKey(queryObject);
        GetItemResult queryResult = client.getItem(getMessageRequest);
        Map<String, AttributeValue> item = queryResult.getItem();

        if (item.isEmpty()) {
            throw new NotFoundException(MESSAGE_NOT_FOUND_ERROR + queryObject.getIdentifier().toString());
        }
        return item;
    }

    private List<Message> parseMessages(QueryResult queryResult) {
        return queryResult.getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, MessageDao.class))
                   .map(MessageDao::getData)
                   .collect(Collectors.toList());
    }

    private QueryRequest queryForFetchingAllMessagesForAUser(MessageDao queryObject) {
        return new QueryRequest()
                   .withTableName(tableName)
                   .withKeyConditions(queryObject.primaryKeyPartitionKeyCondition());
    }

    private QueryRequest queryRequestForListingMessagesByCustomerAndStatus(MessageDao queryObject) {
        return new QueryRequest()
                   .withTableName(tableName)
                   .withIndexName(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
                   .withKeyConditions(queryObject.fetchEntryCollectionByTypeCustomerStatusKey());
    }

    private Message createNewSimpleMessage(UserInstance sender, Publication publication, String messageText

    ) {
        requireMessageIsNotBlank(messageText);
        SortableIdentifier messageIdentifier = identifierSupplier.get();
        return Message.simpleMessage(
            sender,
            publication,
            messageText,
            messageIdentifier,
            clockForTimestamps
        );
    }

    private Message createNewDoiRequestMessage(UserInstance sender, Publication publication, String messageText) {
        requireMessageIsNotBlank(messageText);
        SortableIdentifier messageIdentifier = identifierSupplier.get();
        return Message.doiRequestMessage(
            sender,
            publication,
            messageText,
            messageIdentifier,
            clockForTimestamps
        );
    }

    private void requireMessageIsNotBlank(String messageText) {
        if (StringUtils.isBlank(messageText)) {
            throw new InvalidInputException(EMPTY_MESSAGE_ERROR);
        }
    }

    private QueryRequest queryForRetrievingMessagesAndRespectiveResource(ResourceDao queryObject) {
        String entityType = MessageDao.joinByResourceOrderedContainedType();
        Map<String, Condition> keyCondition = queryObject.byResource(entityType);

        return new QueryRequest()
                   .withTableName(RESOURCES_TABLE_NAME)
                   .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .withKeyConditions(keyCondition);
    }

    private GetItemRequest getMessageByPrimaryKey(MessageDao queryObject) {
        return new GetItemRequest()
                   .withTableName(tableName)
                   .withKey(queryObject.primaryKey());
    }
}
