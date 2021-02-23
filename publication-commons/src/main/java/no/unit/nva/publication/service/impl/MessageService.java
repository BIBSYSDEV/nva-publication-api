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
import java.util.Comparator;
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
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.MessageDao;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public class MessageService extends ServiceWithTransactions {

    public static final String RAWTYPES = "rawtypes";
    public static final String EMPTY_MESSAGE_ERROR = "Message cannot be empty";
    public static final String PATH_SEPARATOR = "/";
    private static final int MESSAGES_BY_RESOURCE_RESULT_RESOURCE_INDEX = 0;
    private static final int MESSAGES_BY_RESOURCE_RESULT_FIRST_MESSAGE_INDEX =
        MESSAGES_BY_RESOURCE_RESULT_RESOURCE_INDEX + 1;
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

    //TODO replace extraction SortableIdentifier.fromUri() when nva-commons it at version 1.1.1.
    public static SortableIdentifier extractIdentifier(URI uri) {
        String[] path = uri.getPath().split(PATH_SEPARATOR);
        return new SortableIdentifier(path[path.length - 1]);
    }

    @JacocoGenerated
    @Deprecated
    public URI createMessage(UserInstance sender,
                             Publication publication,
                             String messageText) throws TransactionFailedException {
        return createSimpleMessage(sender, publication, messageText);
    }

    public URI createDoiRequestMessage(UserInstance sender, Publication publication, String messageText)
        throws TransactionFailedException {
        Message message = createNewDoiRequestMessage(sender, publication, messageText);
        return writeMessageToDb(message);
    }

    public URI writeMessageToDb(Message message) throws TransactionFailedException {
        TransactWriteItem dataWriteItem = newPutTransactionItem(new MessageDao(message));

        IdentifierEntry identifierEntry = new IdentifierEntry(message.getIdentifier().toString());
        TransactWriteItem identifierWriteItem = newPutTransactionItem(identifierEntry);

        TransactWriteItemsRequest request = newTransactWriteItemsRequest(dataWriteItem, identifierWriteItem);
        sendTransactionWriteRequest(request);
        return message.getId();
    }

    public URI createSimpleMessage(UserInstance sender, Publication publication, String messageText)
        throws TransactionFailedException {
        Message message = createNewSimpleMessage(sender, publication, messageText);
        return writeMessageToDb(message);
    }

    public Message getMessage(UserInstance owner, URI messageId) {
        SortableIdentifier identifier = extractIdentifier(messageId);
        return getMessage(owner, identifier);
    }

    public Message getMessage(UserInstance owner, SortableIdentifier identifier) {
        MessageDao queryObject = MessageDao.queryObject(owner, identifier);
        GetItemRequest getMessageRequest = getMessageByPrimaryKey(queryObject);
        GetItemResult queryResult = client.getItem(getMessageRequest);
        Map<String, AttributeValue> item = queryResult.getItem();
        MessageDao result = parseAttributeValuesMap(item, MessageDao.class);
        return result.getData();
    }

    @SuppressWarnings(RAWTYPES)
    public Optional<ResourceMessages> getMessagesForResource(UserInstance user, SortableIdentifier identifier) {
        ResourceDao queryObject = ResourceDao.queryObject(user, identifier);
        QueryRequest queryRequest = queryForRetrievingMessagesByResource(queryObject);
        List<Dao> resultDaos = executeQuery(queryRequest);
        return messagesWithResource(resultDaos);
    }

    public List<Message> listMessages(URI customerId, MessageStatus messageStatus) {
        MessageDao queryObject = MessageDao.listMessagesForCustomerAndStatus(customerId, messageStatus);
        QueryRequest queryRequest = queryRequestForListingMessagesByCustomerAndStatus(queryObject);
        List<MessageDao> queryResult = executeQuery(queryRequest, MessageDao.class);
        return queryResult.stream()
                   .map(MessageDao::getData)
                   .collect(Collectors.toList());
    }

    public List<ResourceMessages> listMessagesForUser(UserInstance owner) {
        MessageDao queryObject = MessageDao.listMessagesAndResourcesForUser(owner);
        QueryRequest queryRequest = queryForFetchingAllMessagesForAUser(queryObject);
        QueryResult queryResult = client.query(queryRequest);
        Map<SortableIdentifier, List<Message>> messagesPerResource = groupMessagesByResourceIdentifier(queryResult);
        return createResponseObjects(messagesPerResource);
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

    private List<ResourceMessages> createResponseObjects(Map<SortableIdentifier, List<Message>> messagesPerResource) {
        return messagesPerResource.values()
                   .stream()
                   .flatMap(messages -> ResourceMessages.fromMessageList(messages).stream())
                   .sorted(Comparator.comparing(resourceMessage -> resourceMessage.getPublication().getIdentifier()))
                   .collect(Collectors.toList());
    }

    private Map<SortableIdentifier, List<Message>> groupMessagesByResourceIdentifier(QueryResult queryResult) {
        return queryResult.getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, MessageDao.class))
                   .map(MessageDao::getData)
                   .collect(Collectors.groupingBy(Message::getResourceIdentifier));
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

    private Optional<ResourceMessages> messagesWithResource(List<Dao> daos) {
        List<Message> messages = extractMessages(daos);
        return ResourceMessages.fromMessageList(messages);
    }

    @SuppressWarnings(RAWTYPES)
    private List<Dao> executeQuery(QueryRequest queryRequest) {
        return executeQuery(queryRequest, Dao.class);
    }

    //TODO: check if these methods can be re-used by other services
    private <T> List<T> executeQuery(QueryRequest queryRequest, Class<T> daoClass) {
        QueryResult result = client.query(queryRequest);
        return extractDaos(result, daoClass);
    }

    private <T> List<T> extractDaos(QueryResult result, Class<T> daoClass) {
        return result.getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, daoClass))
                   .collect(Collectors.toList());
    }

    private QueryRequest queryForRetrievingMessagesByResource(ResourceDao queryObject) {
        Map<String, Condition> keyCondition = queryObject.byResource(
            ResourceDao.joinByResourceContainedOrderedType(),
            MessageDao.joinByResourceOrderedContainedType());
        return new QueryRequest()
                   .withTableName(RESOURCES_TABLE_NAME)
                   .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .withKeyConditions(keyCondition);
    }

    @SuppressWarnings(RAWTYPES)
    private List<Message> extractMessages(List<Dao> daos) {
        return
            daos.subList(MESSAGES_BY_RESOURCE_RESULT_FIRST_MESSAGE_INDEX, daos.size())
                .stream()
                .map(genericDao -> (MessageDao) genericDao)
                .map(MessageDao::getData)
                .collect(Collectors.toList());
    }

    private GetItemRequest getMessageByPrimaryKey(MessageDao queryObject) {
        return new GetItemRequest()
                   .withTableName(tableName)
                   .withKey(queryObject.primaryKey());
    }
}
