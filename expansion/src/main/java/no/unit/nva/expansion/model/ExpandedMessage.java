package no.unit.nva.expansion.model;

import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static no.unit.nva.expansion.model.ExpandedMessage.TYPE;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.WithOrganizationScope;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.MessageType;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(TYPE)
public final class ExpandedMessage implements WithOrganizationScope, ExpandedDataEntry {

    public static final String TYPE = "Message";
    private SortableIdentifier identifier;
    private String owner;
    private URI customerId;
    private MessageStatus status;
    private String sender;
    private SortableIdentifier resourceIdentifier;
    private URI resourceId;
    private String text;
    private Instant createdTime;
    private String resourceTitle;
    private MessageType messageType;
    private Set<URI> organizationIds;

    public ExpandedMessage() {

    }

    public static ExpandedMessage create(Message message, ResourceExpansionService resourceExpansionService)
        throws NotFoundException {
        ExpandedMessage expandedMessage = ExpandedMessage.fromMessage(message);
        Set<URI> organizationIds = resourceExpansionService.getOrganizationIds(message);
        expandedMessage.setOrganizationIds(organizationIds);
        URI resourceId = resourceExpansionService.getResourceId(message.getResourceIdentifier());
        expandedMessage.setResourceId(resourceId);
        return expandedMessage;
    }


    @JacocoGenerated
    public MessageType getMessageType() {
        return messageType;
    }

    @JacocoGenerated
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @JacocoGenerated
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @JacocoGenerated
    public String getOwner() {
        return owner;
    }

    @JacocoGenerated
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JacocoGenerated
    public MessageStatus getStatus() {
        return status;
    }

    @JacocoGenerated
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    @JacocoGenerated
    public String getSender() {
        return sender;
    }

    @JacocoGenerated
    public void setSender(String sender) {
        this.sender = sender;
    }

    @JacocoGenerated
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @JacocoGenerated
    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    @JacocoGenerated
    public URI getResourceId() {
        return resourceId;
    }

    @JacocoGenerated
    public void setResourceId(URI resourceId) {
        this.resourceId = resourceId;
    }

    @JacocoGenerated
    public String getText() {
        return text;
    }

    @JacocoGenerated
    public void setText(String text) {
        this.text = text;
    }

    @JacocoGenerated
    public Instant getCreatedTime() {
        return createdTime;
    }

    @JacocoGenerated
    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    @JacocoGenerated
    public String getResourceTitle() {
        return resourceTitle;
    }

    @JacocoGenerated
    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    @Override
    public Set<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : emptySet();
    }

    @Override
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }

    public Message toMessage() {
        Message message = new Message();
        message.setMessageType(this.getMessageType());
        message.setCreatedTime(this.getCreatedTime());
        message.setIdentifier(this.getIdentifier());
        message.setCustomerId(this.getCustomerId());
        message.setOwner(this.getOwner());
        message.setResourceIdentifier(this.getResourceIdentifier());
        message.setSender(this.getSender());
        message.setResourceTitle(this.getResourceTitle());
        message.setStatus(this.getStatus());
        message.setText(this.getText());
        return message;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getOwner(), getCustomerId(), getStatus(), getSender(),
                            getResourceIdentifier(), getResourceId(),
                            getText(), getCreatedTime(), getResourceTitle(), getMessageType(), getOrganizationIds());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpandedMessage)) {
            return false;
        }
        ExpandedMessage that = (ExpandedMessage) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && getStatus() == that.getStatus()
               && Objects.equals(getSender(), that.getSender())
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && Objects.equals(getResourceId(), that.getResourceId())
               && Objects.equals(getText(), that.getText())
               && Objects.equals(getCreatedTime(), that.getCreatedTime())
               && Objects.equals(getResourceTitle(), that.getResourceTitle())
               && getMessageType() == that.getMessageType()
               && Objects.equals(getOrganizationIds(), that.getOrganizationIds());
    }

    @Override
    public SortableIdentifier retrieveIdentifier() {
        return getIdentifier();
    }

    // should not become public. An ExpandedMessage needs an Expansion service to be complete
    private static ExpandedMessage fromMessage(Message message) {
        ExpandedMessage expandedMessage = new ExpandedMessage();
        expandedMessage.setMessageType(message.getMessageType());
        expandedMessage.setCreatedTime(message.getCreatedTime());
        expandedMessage.setIdentifier(message.getIdentifier());
        expandedMessage.setCustomerId(message.getCustomerId());
        expandedMessage.setOwner(message.getOwner());
        expandedMessage.setResourceIdentifier(message.getResourceIdentifier());
        expandedMessage.setSender(message.getSender());
        expandedMessage.setResourceTitle(message.getResourceTitle());
        expandedMessage.setStatus(message.getStatus());
        expandedMessage.setText(message.getText());
        return expandedMessage;
    }
}
