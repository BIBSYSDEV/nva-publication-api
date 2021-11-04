package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import no.unit.nva.expansion.model.ExpandedDatabaseEntry;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class PersistedDocument implements JsonSerializable {

    public static final String BODY = "body";
    public static final String METADATA = "metadata";
    @JsonProperty(METADATA)
    private final PersistedDocumentMetadata metadata;
    @JsonProperty(BODY)
    private final ExpandedDatabaseEntry body;

    @JsonCreator
    public PersistedDocument(@JsonProperty(BODY) ExpandedDatabaseEntry body,
                             @JsonProperty(METADATA) PersistedDocumentMetadata metadata) {

        this.metadata = metadata;
        this.body = body;
    }

    public static PersistedDocument createIndexDocument(ExpandedDatabaseEntry expandedResourceUpdate) {
        PersistedDocumentMetadata metadata = PersistedDocumentMetadata.createMetadata(expandedResourceUpdate);
        return new PersistedDocument(expandedResourceUpdate, metadata);
    }

    public static PersistedDocument fromJsonString(String indexingEventPayload) throws JsonProcessingException {
        return dynamoImageSerializerRemovingEmptyFields.readValue(indexingEventPayload, PersistedDocument.class);
    }

    @JacocoGenerated
    public PersistedDocumentMetadata getMetadata() {
        return metadata;
    }

    @JacocoGenerated
    public ExpandedDatabaseEntry getBody() {
        return body;
    }

    @Override
    public String toJsonString() {
        return attempt(() -> dynamoImageSerializerRemovingEmptyFields.writeValueAsString(this)).orElseThrow();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), getBody());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistedDocument)) {
            return false;
        }
        PersistedDocument that = (PersistedDocument) o;
        return Objects.equals(getMetadata(), that.getMetadata()) && Objects.equals(getBody(),
                                                                                   that.getBody());
    }
}
