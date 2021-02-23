package no.unit.nva.publication.storage.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.StorageModelConstants.PATH_SEPARATOR;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@Data
@lombok.Builder(
    builderClassName = "DoiRequestBuilder",
    builderMethodName = "builder",
    toBuilder = true,
    setterPrefix = "with")
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Message implements WithIdentifier,
                                WithStatus,
                                RowLevelSecurity,
                                ResourceUpdate,
                                ConnectedToResource {

    public static final String MESSAGE_PATH = StorageModelConstants.getInstance().messagePath;
    private SortableIdentifier identifier;
    private URI id;
    private String owner;
    private URI customerId;
    private MessageStatus status;
    private String sender;
    private boolean isDoiRequestRelated;
    private SortableIdentifier resourceIdentifier;
    private String text;
    private Instant createdTime;
    private String resourceTitle;

    @JacocoGenerated
    public Message() {

    }

    public static Message simpleMessage(UserInstance sender,
                                        Publication publication,
                                        String messageText,
                                        SortableIdentifier messageIdentifier,
                                        Clock clock) {
        return Message.builder()
                   .withStatus(MessageStatus.UNREAD)
                   .withResourceIdentifier(publication.getIdentifier())
                   .withCustomerId(sender.getOrganizationUri())
                   .withText(messageText)
                   .withSender(sender.getUserIdentifier())
                   .withOwner(publication.getOwner())
                   .withResourceTitle(extractTitle(publication))
                   .withIsDoiRequestRelated(false)
                   .withCreatedTime(clock.instant())
                   .withIdentifier(messageIdentifier)
                   .withId(messageId(messageIdentifier))

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

    public static URI messageId(SortableIdentifier messageIdentifier) {
        if (nonNull(messageIdentifier)) {
            String scheme = StorageModelConstants.getInstance().scheme;
            String host = StorageModelConstants.getInstance().host;
            String messagePath = MESSAGE_PATH + PATH_SEPARATOR + messageIdentifier.toString();
            return attempt(() -> newUri(scheme, host, messagePath)).orElseThrow();
        }
        return null;
    }

    private static URI newUri(String scheme, String host, String messagesPath) throws URISyntaxException {
        return new URI(scheme, host, messagesPath, StorageModelConstants.URI_EMPTY_FRAGMENT);
    }

    private static String extractTitle(Publication publication) {
        return Optional.of(publication)
                   .map(Publication::getEntityDescription)
                   .map(EntityDescription::getMainTitle)
                   .orElse(null);
    }
}
