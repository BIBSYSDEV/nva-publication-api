package no.unit.nva.publication.queue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public final class RecoveryEntry {

    public static final String RESOURCE = "Resource";
    public static final String TICKET = "Ticket";
    public static final String MESSAGE = "Message";
    public static final String FILE = "File";
    private static final String ID = "id";
    private static final String RECOVERY_QUEUE = "RECOVERY_QUEUE";
    private static final String TYPE = "type";
    private static final String DATA_TYPE_STRING = "String";
    private final SortableIdentifier identifier;
    private final String type;
    private final String exception;

    private RecoveryEntry(SortableIdentifier identifier, String type, String exception) {
        this.identifier = identifier;
        this.type = type;
        this.exception = exception;
    }

    public static RecoveryEntry create(String type, SortableIdentifier identifier) {
        return builder().withType(type).withIdentifier(identifier).build();
    }

    public void persist(QueueClient queueClient) {
        queueClient.sendMessage(createSendMessageRequest());
    }

    public RecoveryEntry withException(Exception exception) {
        return this.copy().withException(getStackTrace(exception)).build();
    }

    private static Builder builder() {
        return new Builder();
    }

    private SendMessageRequest createSendMessageRequest() {
        return SendMessageRequest.builder()
                   .messageAttributes(Map.of(ID, convertToMessageAttribute(identifier.toString()), TYPE,
                                             convertToMessageAttribute(type)))
                   .messageBody(exception)
                   .queueUrl(new Environment().readEnv(RECOVERY_QUEUE))
                   .build();
    }

    private MessageAttributeValue convertToMessageAttribute(String value) {
        return MessageAttributeValue.builder().stringValue(value).dataType(DATA_TYPE_STRING).build();
    }

    private String getStackTrace(Exception exception) {
        var stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private Builder copy() {
        return new Builder().withIdentifier(this.identifier).withType(this.type).withException(this.exception);
    }

    private static final class Builder {

        private SortableIdentifier identifier;
        private String type;
        private String failure;

        private Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withException(String failure) {
            this.failure = failure;
            return this;
        }

        public RecoveryEntry build() {
            return new RecoveryEntry(identifier, type, failure);
        }
    }
}
