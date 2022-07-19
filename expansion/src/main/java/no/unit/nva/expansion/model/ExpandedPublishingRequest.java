package no.unit.nva.expansion.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
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
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

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
    @JsonProperty("organizationIds")
    private Set<URI> organizationIds;
    
    public ExpandedPublishingRequest() {
        this.messages = MessageCollection.empty(MessageType.PUBLISHING_REQUEST);
    }
    
    public static ExpandedPublishingRequest create(PublishingRequestCase publishingRequestCase,
                                                   ResourceService resourceService,
                                                   MessageService messageService,
                                                   ResourceExpansionService resourceExpansionService)
        throws NotFoundException {
        var userInstance =
            UserInstance.create(publishingRequestCase.getOwner(), publishingRequestCase.getCustomerId());
        var messageCollection =
            fetchAllMessagesForPublishingRequestCase(publishingRequestCase, messageService, userInstance);
        var publication = fetchPublication(publishingRequestCase, resourceService);
        var organizationIds = resourceExpansionService.getOrganizationIds(publishingRequestCase);
        return create(publishingRequestCase, publication, messageCollection, organizationIds);
    }
    
    @JacocoGenerated
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }
    
    @JacocoGenerated
    @Override
    public String toJsonString() {
        return ExpandedTicket.super.toJsonString();
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return getIdentifier();
    }
    
    @JacocoGenerated
    public MessageCollection getMessages() {
        return isNull(messages) ? MessageCollection.empty(MessageType.PUBLISHING_REQUEST) : messages;
    }
    
    public void setMessages(MessageCollection messages) {
        this.messages = messages;
    }
    
    @Override
    public PublicationSummary getPublicationSummary() {
        return this.publicationSummary;
    }
    
    @Override
    public Set<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : Collections.emptySet();
    }
    
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }
    
    public void setPublicationSummary(PublicationSummary publicationSummary) {
        this.publicationSummary = publicationSummary;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getPublicationSummary(), getMessages());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpandedPublishingRequest)) {
            return false;
        }
        ExpandedPublishingRequest that = (ExpandedPublishingRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier()) && Objects.equals(
            getPublicationSummary(), that.getPublicationSummary()) && Objects.equals(getMessages(),
            that.getMessages());
    }
    
    private static ExpandedPublishingRequest create(PublishingRequestCase dataEntry,
                                                    Publication publication,
                                                    MessageCollection messages,
                                                    Set<URI> organizationIds) {
        var entry = new ExpandedPublishingRequest();
        entry.setIdentifier(dataEntry.getIdentifier());
        entry.setPublicationSummary(PublicationSummary.create(publication));
        entry.setMessages(messages);
        entry.setOrganizationIds(organizationIds);
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
