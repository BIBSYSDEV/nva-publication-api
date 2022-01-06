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
    private String text;
    private Instant createdTime;
    private PublicationSummary publicationSummary;
    private MessageType messageType;
    private Set<URI> organizationIds;

    public ExpandedMessage() {

    }

    public static ExpandedMessage create(Message message, ResourceExpansionService resourceExpansionService)
        throws NotFoundException {
        ExpandedMessage expandedMessage = ExpandedMessage.fromMessage(message);
        Set<URI> organizationIds = resourceExpansionService.getOrganizationIds(message);
        expandedMessage.setOrganizationIds(organizationIds);
        return expandedMessage;
    }

    @JacocoGenerated
    public PublicationSummary getPublicationSummary() {
        return publicationSummary;
    }

    @JacocoGenerated
    public void setPublicationSummary(PublicationSummary publicationSummary) {
        this.publicationSummary = publicationSummary;
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
        message.setMessageType(getMessageType());
        message.setCreatedTime(getCreatedTime());
        message.setIdentifier(getIdentifier());
        message.setCustomerId(getCustomerId());
        message.setOwner(getOwner());
        message.setResourceIdentifier(SortableIdentifier.fromUri(this.getPublicationSummary().getId()));
        message.setSender(getSender());
        message.setResourceTitle(getPublicationSummary().getTitle());
        message.setStatus(this.getStatus());
        message.setText(this.getText());
        return message;
    }



    @Override
    public SortableIdentifier retrieveIdentifier() {
        return getIdentifier();
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
               && Objects.equals(getText(), that.getText())
               && Objects.equals(getCreatedTime(), that.getCreatedTime())
               && Objects.equals(getPublicationSummary(), that.getPublicationSummary())
               && getMessageType() == that.getMessageType()
               && Objects.equals(getOrganizationIds(), that.getOrganizationIds());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getOwner(), getCustomerId(), getStatus(), getSender(), getText(),
                            getCreatedTime(), getPublicationSummary(), getMessageType(), getOrganizationIds());
    }

    // should not become public. An ExpandedMessage needs an Expansion service to be complete
    private static ExpandedMessage fromMessage(Message message) {
        ExpandedMessage expandedMessage = new ExpandedMessage();
        expandedMessage.setPublicationSummary(PublicationSummary.create(message));
        expandedMessage.setMessageType(message.getMessageType());
        expandedMessage.setCreatedTime(message.getCreatedTime());
        expandedMessage.setIdentifier(message.getIdentifier());
        expandedMessage.setCustomerId(message.getCustomerId());
        expandedMessage.setOwner(message.getOwner());
        expandedMessage.setSender(message.getSender());
        expandedMessage.setStatus(message.getStatus());
        expandedMessage.setText(message.getText());
        return expandedMessage;
    }
}
