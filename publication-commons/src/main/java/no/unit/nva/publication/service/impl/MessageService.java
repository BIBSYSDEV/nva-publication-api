package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.Dao.parseAttributeValuesMap;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.MessageDao;
import no.unit.nva.publication.storage.model.daos.ResourceDao;

public class MessageService {
    
    private static final int MESSAGES_BY_RESOURCE_RESULT_RESOURCE_INDEX = 0;
    private static final int MESSAGES_BY_RESOURCE_RESULT_FIRST_MESSAGE_INDEX =
        MESSAGES_BY_RESOURCE_RESULT_RESOURCE_INDEX + 1;
    private final AmazonDynamoDB client;
    private final String tableName;
    private final Clock clockForTimestamps;
    
    public MessageService(AmazonDynamoDB client, Clock clockForTimestamps) {
        this.client = client;
        tableName = RESOURCES_TABLE_NAME;
        this.clockForTimestamps = clockForTimestamps;
    }
    
    public SortableIdentifier createMessage(UserInstance sender,
                                            UserInstance owner,
                                            SortableIdentifier resourceIdentifier,
                                            String messageText) {
        Message message = Message.simpleMessage(sender, owner, resourceIdentifier, messageText, clockForTimestamps);
        MessageDao dao = new MessageDao(message);
        PutItemRequest putItemRequest = new PutItemRequest()
                                            .withTableName(tableName)
                                            .withItem(dao.toDynamoFormat());
        client.putItem(putItemRequest);
        return dao.getIdentifier();
    }
    
    public Message getMessage(UserInstance owner, SortableIdentifier identifier) {
        MessageDao queryObject = MessageDao.queryObject(owner, identifier);
        GetItemRequest getMessageRequest = getMessageByPrimaryKey(queryObject);
        GetItemResult queryResult = client.getItem(getMessageRequest);
        Map<String, AttributeValue> item = queryResult.getItem();
        MessageDao result = parseAttributeValuesMap(item, MessageDao.class);
        return result.getData();
    }
    
    public ResourceMessages getMessagesForResource(UserInstance user, SortableIdentifier identifier) {
        ResourceDao queryObject = ResourceDao.queryObject(user, identifier);
        QueryRequest queryRequest = queryForRetrievingMessagesByResource(queryObject);
        List<Dao<?>> daos = executeQuery(queryRequest);
        return messagesWithResource(daos);
    }
    
    private ResourceMessages messagesWithResource(List<Dao<?>> daos) {
        Resource resource = extractResource(daos);
        List<Message> messages = extractMessages(daos);
        return new ResourceMessages(resource, messages);
    }
    
    private List<Dao<?>> executeQuery(QueryRequest queryRequest) {
        QueryResult result = client.query(queryRequest);
        return extractDaos(result);
    }
    
    @SuppressWarnings("unchecked")
    private List<Dao<?>> extractDaos(QueryResult result) {
        return result.getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, Dao.class))
                   .collect(Collectors.toList());
    }
    
    private QueryRequest queryForRetrievingMessagesByResource(ResourceDao queryObject) {
        Map<String, Condition> keyCondition = queryObject.byResource(
            ResourceDao.joinByResourceContainedOrderedType(),
            MessageDao.joinByResourceOrderedContainedType());
        QueryRequest queryRequest = new QueryRequest()
                                        .withTableName(RESOURCES_TABLE_NAME)
                                        .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
                                        .withKeyConditions(keyCondition);
        return queryRequest;
    }
    
    private List<Message> extractMessages(List<Dao<?>> daos) {
        return
            daos.subList(MESSAGES_BY_RESOURCE_RESULT_FIRST_MESSAGE_INDEX, daos.size())
                .stream()
                .map(genericDao -> (MessageDao) genericDao)
                .map(MessageDao::getData)
                .collect(Collectors.toList());
    }
    
    private Resource extractResource(List<Dao<?>> daos) {
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
