package no.unit.nva.publication.service.impl;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
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
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.MessageDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import nva.commons.apigateway.exceptions.NotFoundException;
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
    
    public SortableIdentifier createMessage(UserInstance sender,
                                            Publication publication,
                                            String messageText,
                                            MessageType messageType) {
        requireMessageIsNotBlank(messageText);
    
        Message message = createMessageEntry(sender, publication, messageText, messageType);
        return writeMessageToDb(message);
    }
    
    public Message createMessage(TicketEntry ticketEntry, UserInstance userInstance, String messageText) {
        var newMessage = Message.create(ticketEntry, userInstance, messageText);
        var dao = new MessageDao(newMessage);
        var transactionRequest = dao.createInsertionTransactionRequest();
        client.transactWriteItems(transactionRequest);
        return fetchEventualConsistentDataEntry(newMessage, this::getMessageByIdentifier).orElseThrow();
    }
    
    public Optional<Message> getMessageByIdentifier(SortableIdentifier identifier) {
        var queryObject = new MessageDao(Message.builder().withIdentifier(identifier).build());
        return attempt(() -> queryObject.fetchByIdentifier(client, MessageDao.class))
                   .map(MessageDao::getData)
                   .toOptional();
    }
    
    private Message getMessageByIdentifier(Message message) {
        return getMessageByIdentifier(message.getIdentifier()).orElseThrow();
    }
    
    /* TODO: consider using UpdateRequest to avoid doing the existence checking beforehand. At the present time
       we are doing 2 reads and 1 write to update the message status. If we did the update, we should be careful
       to update the indices as well.
     */
    public Message markAsRead(Message message) throws NotFoundException {
        var existingMessage = getMessage(UserInstance.fromMessage(message), message.getIdentifier());
        var messageUpdate = existingMessage.markAsRead(clockForTimestamps);
        var dao = new MessageDao(messageUpdate);
        var putRequest = new PutItemRequest()
                             .withTableName(RESOURCES_TABLE_NAME)
                             .withItem(dao.toDynamoFormat());
        client.putItem(putRequest);
        return messageUpdate;
    }
    
    private SortableIdentifier writeMessageToDb(Message message) {
        TransactWriteItem dataWriteItem = newPutTransactionItem(new MessageDao(message));
        
        IdentifierEntry identifierEntry = new IdentifierEntry(message.getIdentifier().toString());
        TransactWriteItem identifierWriteItem = newPutTransactionItem(identifierEntry);
        
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(dataWriteItem, identifierWriteItem);
        sendTransactionWriteRequest(request);
        return message.getIdentifier();
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
    
    public List<ResourceConversation> listMessagesForCurator(URI customerId, TicketStatus ticketStatus) {
        MessageDao queryObject = MessageDao.listMessagesForCustomerAndStatus(customerId, ticketStatus);
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
    protected AmazonDynamoDB getClient() {
        return client;
    }
    
    private static Supplier<SortableIdentifier> defaultIdentifierSupplier() {
        return SortableIdentifier::next;
    }
    
    private Map<String, AttributeValue> fetchMessage(MessageDao queryObject) throws NotFoundException {
        
        GetItemRequest getMessageRequest = getMessageByPrimaryKey(queryObject);
        GetItemResult queryResult = client.getItem(getMessageRequest);
        Map<String, AttributeValue> item = queryResult.getItem();
        
        if (isNull(item) || item.isEmpty()) {
            throw new NotFoundException(MESSAGE_NOT_FOUND_ERROR + queryObject.getIdentifier().toString());
        }
        return item;
    }
    
    private List<Message> parseMessages(QueryResult queryResult) {
        return queryResult.getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, MessageDao.class))
                   .map(MessageDao::getData)
                   .map(Message.class::cast)
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
    
    private Message createMessageEntry(UserInstance sender,
                                       Publication publication,
                                       String messageText,
                                       MessageType messageType) {
        return Message.create(sender,
            publication,
            messageText,
            identifierSupplier.get(),
            clockForTimestamps,
            messageType);
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
