package no.unit.nva.expansion.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.MessageCollection;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.UserInstance;

public class ExpandedPublishingRequest implements ExpandedTicket {
    
    public static final String TYPE = "PublishingRequest";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String MESSAGES_FIELD = "messages";
    
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(PUBLICATION_FIELD)
    private PublicationSummary publicationSummary;
    @JsonProperty(MESSAGES_FIELD)
    private MessageCollection messages;
    
    public static ExpandedPublishingRequest create(PublishingRequestCase publishingRequestCase,
                                                   ResourceService resourceService,
                                                   MessageService messageService) {
        var userInstance = UserInstance.create(publishingRequestCase.getOwner(), publishingRequestCase.getCustomerId());
        var messageCollection = fetchAllMessagesForPublishingRequestCase(publishingRequestCase,
            messageService, userInstance);
        var publication = fetchPublication(publishingRequestCase, resourceService);
        return ExpandedPublishingRequest.create(publishingRequestCase, publication, messageCollection);
    }
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }
    
    @Override
    public String toJsonString() {
        return ExpandedTicket.super.toJsonString();
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return getIdentifier();
    }
    
    public MessageCollection getMessages() {
        return messages;
    }
    
    public void setMessages(MessageCollection messages) {
        this.messages = messages;
    }
    
    @Override
    public PublicationSummary getPublicationSummary() {
        return this.publicationSummary;
    }
    
    public void setPublicationSummary(PublicationSummary publicationSummary) {
        this.publicationSummary = publicationSummary;
    }
    
    private static ExpandedPublishingRequest create(PublishingRequestCase dataEntry,
                                                    Publication publication,
                                                    MessageCollection messages) {
        var entry = new ExpandedPublishingRequest();
        entry.setIdentifier(dataEntry.getIdentifier());
        entry.setPublicationSummary(PublicationSummary.create(publication));
        entry.setMessages(messages);
        return entry;
    }
    
    private static Publication fetchPublication(PublishingRequestCase publishingRequestCase,
                                                ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(publishingRequestCase.getResourceIdentifier()))
            .orElseThrow();
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
