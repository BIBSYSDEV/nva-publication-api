package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.MessageCollection;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.UserInstance;

public class ExpandedPublishingRequest implements ExpandedDataEntry {
    
    public static final String TYPE = "PublishingRequest";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String DATA_FIELD = "data";
    public static final String MESSAGES_FIELD = "messages";
    
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(DATA_FIELD)
    private final PublishingRequestCase dataEntry;
    @JsonProperty(MESSAGES_FIELD)
    private final MessageCollection messages;
    
    public ExpandedPublishingRequest(PublishingRequestCase dataEntry, MessageCollection messages) {
        this.dataEntry = dataEntry;
        this.identifier = dataEntry.getIdentifier();
        this.messages = messages;
    }
    
    public static ExpandedPublishingRequest create(PublishingRequestCase publishingRequestCase,
                                                   MessageService messageService) {
        var userInstance = UserInstance.create(publishingRequestCase.getOwner(), publishingRequestCase.getCustomerId());
        
        MessageCollection messageCollection = fetchAllMessagesForPublishingRequestCase(publishingRequestCase,
            messageService, userInstance);
        return new ExpandedPublishingRequest(publishingRequestCase, messageCollection);
    }
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    @Override
    public String toJsonString() {
        return ExpandedDataEntry.super.toJsonString();
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return dataEntry.getIdentifier();
    }
    
    public MessageCollection getMessages() {
        return messages;
    }
    
    private static MessageCollection fetchAllMessagesForPublishingRequestCase(
        PublishingRequestCase publishingRequestCase,
        MessageService messageService, UserInstance userInstance) {
        return messageService.getMessagesForResource(userInstance, publishingRequestCase.getResourceIdentifier())
            .map(ExpandedPublishingRequest::retainPublishingRequestMessages)
            .orElse(MessageCollection.empty(MessageType.PUBLISHING_REQUEST));
    }
    
    private static MessageCollection retainPublishingRequestMessages(ResourceConversation conversation) {
        return conversation.getMessageCollectionOfType(MessageType.PUBLISHING_REQUEST);
    }
}
