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
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.MessageDao;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageService extends ServiceWithTransactions {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    public static final String SENDER_OR_CURATOR_ONLY_MESSAGE = "Only message sender and curator can perform this "
                                                               + "action!";
    public static final String MESSAGE_NOT_FOUND_ERROR = "Could not find message with identifier:";
    private static final String MESSAGE_REFRESHED_MESSAGE =
        "Message {} for ticket {} for publication {} has been refreshed successfully";
    public static final String MESSAGE_TO_REFRESH_NOT_FOUND_MESSAGE = "Could not refresh message: {}";
    private final String tableName;
    
    private final TicketService ticketService;
    
    public MessageService(AmazonDynamoDB client, UriRetriever uriRetriever) {
        super(client);
        this.ticketService = new TicketService(client, uriRetriever);
        tableName = RESOURCES_TABLE_NAME;
    }
    
    @JacocoGenerated
    public static MessageService defaultService() {
        return new MessageService(DEFAULT_DYNAMODB_CLIENT, UriRetriever.defaultUriRetriever());
    }
    
    public Message createMessage(TicketEntry ticketEntry, UserInstance sender, String messageText) {
        var newMessage = Message.create(ticketEntry, sender, messageText);
        var dao = newMessage.toDao();
        var transactionRequest = dao.createInsertionTransactionRequest();
        getClient().transactWriteItems(transactionRequest);
        
        markTicketUnreadForEveryoneExceptSender(ticketEntry, sender);
        return fetchEventualConsistentDataEntry(newMessage, this::extractMessageByIdentifier).orElseThrow();
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

    public void deleteMessage(UserInstance userInstance, Message message)
        throws UnauthorizedException, NotFoundException {
        if (!canManageMessage(message, userInstance)) {
            throw new UnauthorizedException(SENDER_OR_CURATOR_ONLY_MESSAGE);
        }
        message.setModifiedDate(Instant.now());
        message.setStatus(MessageStatus.DELETED);
        var dao = message.toDao();
        var transactionRequest = dao.createInsertionTransactionRequest();
        getClient().transactWriteItems(transactionRequest);
    }

    private boolean canManageMessage(Message message, UserInstance userInstance) throws NotFoundException {
        if (userInstance.isSender(message)) {
            return true;
        }

        return isCuratorForMessage(message, userInstance);
    }

    private boolean isCuratorForMessage(Message message, UserInstance userInstance) throws NotFoundException {
        if (!message.getCustomerId().equals(userInstance.getCustomerId())) {
            return false;
        }

        var ticket = ticketService.fetchTicketByIdentifier(message.getTicketIdentifier());
        return switch (ticket) {
            case PublishingRequestCase publishingRequest -> userInstance.getAccessRights()
                                                                .contains(AccessRight.MANAGE_PUBLISHING_REQUESTS);
            case GeneralSupportRequest supportRequest -> userInstance.getAccessRights().contains(AccessRight.SUPPORT);
            case DoiRequest doiRequest -> userInstance.getAccessRights().contains(AccessRight.MANAGE_DOI);
            case UnpublishRequest unpublishRequest -> userInstance.getAccessRights()
                                                          .contains(AccessRight.MANAGE_PUBLISHING_REQUESTS);
            default -> false;
        };
    }

    public void refresh(SortableIdentifier identifier) {
        try {
            var message = getMessageByIdentifier(identifier).orElseThrow();
            message.toDao().updateExistingEntry(client);
            logger.info(MESSAGE_REFRESHED_MESSAGE,
                        message.getIdentifier(),
                        message.getTicketIdentifier(),
                        message.getResourceIdentifier());
        } catch (Exception e) {
            logger.info(MESSAGE_TO_REFRESH_NOT_FOUND_MESSAGE, identifier);
        }
    }

    private Message extractMessageByIdentifier(Message message) {
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
            throw new NotFoundException(MESSAGE_NOT_FOUND_ERROR + queryObject.getIdentifier().toString());
        }
        return item;
    }
    
    private GetItemRequest getMessageByPrimaryKey(MessageDao queryObject) {
        return new GetItemRequest()
                   .withTableName(tableName)
                   .withKey(queryObject.primaryKey());
    }
}
