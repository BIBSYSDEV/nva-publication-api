package no.unit.nva.publication.service.impl;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.MessageDao;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class MessageService extends ServiceWithTransactions {
    
    public static final String MESSAGE_NOT_FOUND_ERROR = "Could not find message with identifier:";
    public static final String FOR_USER = " for user: ";

    private final String tableName;
    
    private final TicketService ticketService;
    
    public MessageService(AmazonDynamoDB client) {
        super(client);
        this.ticketService = new TicketService(client);
        tableName = RESOURCES_TABLE_NAME;
    }
    
    @JacocoGenerated
    public static MessageService defaultService() {
        return new MessageService(DEFAULT_DYNAMODB_CLIENT);
    }
    
    public Message createMessage(TicketEntry ticketEntry, UserInstance sender, String messageText) {
        var newMessage = Message.create(ticketEntry, sender, messageText);
        var dao = newMessage.toDao();
        var transactionRequest = dao.createInsertionTransactionRequest();
        getClient().transactWriteItems(transactionRequest);
        
        markTicketUnreadForEveryoneExceptSender(ticketEntry, sender);
        return fetchEventualConsistentDataEntry(newMessage, this::getMessageByIdentifier).orElseThrow();
    }
    
    public Message getMessage(UserInstance owner, SortableIdentifier identifier) throws NotFoundException {
        MessageDao queryObject = MessageDao.queryObject(owner, identifier);
        Map<String, AttributeValue> item = fetchMessage(queryObject);
        MessageDao result = parseAttributeValuesMap(item, MessageDao.class);
        return (Message) result.getData();
    }
    
    public Optional<Message> getMessageByIdentifier(SortableIdentifier identifier) {
        var queryObject = new MessageDao(Message.builder().withIdentifier(identifier).build());
        return attempt(() -> queryObject.fetchByIdentifier(getClient(), tableName))
                   .map(Dao::getData)
                   .map(Message.class::cast)
                   .toOptional();
    }

    public void deleteMessage(UserInstance userInstance, SortableIdentifier messageIdentifier)
        throws NotFoundException {
        var message = getMessage(userInstance, messageIdentifier);
        message.setModifiedDate(Instant.now());
        message.setStatus(MessageStatus.DELETED);
        var dao = message.toDao();
        var transactionRequest = dao.createInsertionTransactionRequest();
        getClient().transactWriteItems(transactionRequest);
    }

    private Message getMessageByIdentifier(Message message) {
        return getMessageByIdentifier(message.getIdentifier()).orElseThrow();
    }
    
    private void markTicketUnreadForEveryoneExceptSender(TicketEntry ticketEntry, UserInstance sender) {
        ticketEntry.markUnreadForEveryone().markReadByUser(sender.getUser()).persistUpdate(ticketService);
    }
    
    private Map<String, AttributeValue> fetchMessage(MessageDao queryObject) throws NotFoundException {
    
        GetItemRequest getMessageRequest = getMessageByPrimaryKey(queryObject);
        GetItemResult queryResult = getClient().getItem(getMessageRequest);
        Map<String, AttributeValue> item = queryResult.getItem();
        
        if (isNull(item) || item.isEmpty()) {
            throw new NotFoundException(MESSAGE_NOT_FOUND_ERROR
                                        + queryObject.getIdentifier().toString()
                                        + FOR_USER
                                        + queryObject.getData().getOwner().toString());
        }
        return item;
    }
    
    private GetItemRequest getMessageByPrimaryKey(MessageDao queryObject) {
        return new GetItemRequest()
                   .withTableName(tableName)
                   .withKey(queryObject.primaryKey());
    }
}
