package no.unit.nva.publication.storage.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
public class Message implements WithIdentifier,
                                WithStatus,
                                RowLevelSecurity,
                                ResourceUpdate,
                                ConnectedToResource {


    private SortableIdentifier identifier;
    private String owner;
    private URI customerId;
    private MessageStatus status;
    private String sender;
    private boolean doiRequestRelated;
    private SortableIdentifier resourceIdentifier;
    private String text;
    private Instant createdTime;
    private String resourceTitle;

    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public URI getCustomerId() {
        return customerId;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public boolean isDoiRequestRelated() {
        return doiRequestRelated;
    }

    public void setDoiRequestRelated(boolean doiRequestRelated) {
        this.doiRequestRelated = doiRequestRelated;
    }

    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    @JacocoGenerated
    public Message() {

    }

    public static Message doiRequestMessage(UserInstance sender,
                                            Publication publication,
                                            String messageText,
                                            SortableIdentifier messageIdentifier,
                                            Clock clock) {
        return buildMessage(sender, publication, messageText, messageIdentifier, clock)
                   .withDoiRequestRelated(true)
                   .build();
    }

    public static Message simpleMessage(UserInstance sender,
                                        Publication publication,
                                        String messageText,
                                        SortableIdentifier messageIdentifier,
                                        Clock clock) {
        return buildMessage(sender, publication, messageText, messageIdentifier, clock)
                   .withDoiRequestRelated(false)
                   .build();
    }

    @Deprecated
    public static Message simpleMessage(UserInstance sender,
                                        Publication publication,
                                        String messageText,
                                        Clock clock) {
        return simpleMessage(sender, publication, messageText, null, clock);
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }

    @Override
    public Publication toPublication() {
        throw new UnsupportedOperationException();
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return attempt(() -> JsonUtils.objectMapper.writeValueAsString(this)).orElseThrow();
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getOwner(), getCustomerId(), getStatus(), getSender(),
                            isDoiRequestRelated(),
                            getResourceIdentifier(), getText(), getCreatedTime(), getResourceTitle());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message)) {
            return false;
        }
        Message message = (Message) o;
        return isDoiRequestRelated() == message.isDoiRequestRelated()
               && Objects.equals(getIdentifier(), message.getIdentifier())
               && Objects.equals(getOwner(), message.getOwner())
               && Objects.equals(getCustomerId(), message.getCustomerId())
               && getStatus() == message.getStatus()
               && Objects.equals(getSender(), message.getSender())
               && Objects.equals(getResourceIdentifier(), message.getResourceIdentifier())
               && Objects.equals(getText(), message.getText())
               && Objects.equals(getCreatedTime(), message.getCreatedTime())
               && Objects.equals(getResourceTitle(), message.getResourceTitle());
    }

    private static MessageBuilder buildMessage(UserInstance sender, Publication publication,
                                               String messageText, SortableIdentifier messageIdentifier,
                                               Clock clock) {
        return Message.builder()
                   .withStatus(MessageStatus.UNREAD)
                   .withResourceIdentifier(publication.getIdentifier())
                   .withCustomerId(sender.getOrganizationUri())
                   .withText(messageText)
                   .withSender(sender.getUserIdentifier())
                   .withOwner(publication.getOwner())
                   .withResourceTitle(extractTitle(publication))
                   .withCreatedTime(clock.instant())
                   .withIdentifier(messageIdentifier);
    }

    private static String extractTitle(Publication publication) {
        return Optional.of(publication)
                   .map(Publication::getEntityDescription)
                   .map(EntityDescription::getMainTitle)
                   .orElse(null);
    }
}
