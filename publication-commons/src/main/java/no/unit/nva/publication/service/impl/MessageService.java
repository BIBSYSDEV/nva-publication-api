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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.MessageDao;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.core.JacocoGenerated;

public class MessageService extends ServiceWithTransactions {

    public static final String RAWTYPES = "rawtypes";

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

    public SortableIdentifier createMessage(UserInstance sender,
                                            UserInstance owner,
                                            SortableIdentifier resourceIdentifier,
                                            String messageText) throws TransactionFailedException {
        Message message = createNewMessage(sender, owner, resourceIdentifier, messageText);
        TransactWriteItem dataWriteItem = newPutTransactionItem(new MessageDao(message));

        IdentifierEntry identifierEntry = new IdentifierEntry(message.getIdentifier().toString());
        TransactWriteItem identifierWriteItem = newPutTransactionItem(identifierEntry);

        TransactWriteItemsRequest request = newTransactWriteItemsRequest(dataWriteItem, identifierWriteItem);
        sendTransactionWriteRequest(request);
        return message.getIdentifier();
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
    public ResourceMessages getMessagesForResource(UserInstance user, SortableIdentifier identifier) {
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


    private QueryRequest queryRequestForListingMessagesByCustomerAndStatus(MessageDao queryObject) {
        return new QueryRequest()
                   .withTableName(tableName)
                   .withIndexName(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
                   .withKeyConditions(queryObject.fetchEntryCollectionByTypeCustomerStatusKey());
    }

    private Message createNewMessage(UserInstance sender, UserInstance owner, SortableIdentifier resourceIdentifier,
                                     String messageText) {
        Message message = Message.simpleMessage(sender, owner, resourceIdentifier, messageText, clockForTimestamps);
        message.setIdentifier(identifierSupplier.get());
        return message;
    }

    private ResourceMessages messagesWithResource(List<Dao> daos) {
        Resource resource = extractResource(daos);
        List<Message> messages = extractMessages(daos);
        return new ResourceMessages(resource, messages);
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
        String searchStartPoint = ResourceDao.joinByResourceContainedOrderedType();
        String searchEndingPoint = MessageDao.joinByResourceOrderedContainedType();
        Map<String, Condition> keyCondition = queryObject.byResource(searchStartPoint, searchEndingPoint);

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

    @SuppressWarnings(RAWTYPES)
    private Resource extractResource(List<Dao> daos) {
        Dao<?> dao = daos.get(MESSAGES_BY_RESOURCE_RESULT_RESOURCE_INDEX);
        ResourceDao resourceDao = (ResourceDao) dao;
        return resourceDao.getData();
    }

    private GetItemRequest getMessageByPrimaryKey(MessageDao queryObject) {
        return new GetItemRequest()
                   .withTableName(tableName)
                   .withKey(queryObject.primaryKey());
    }
}
