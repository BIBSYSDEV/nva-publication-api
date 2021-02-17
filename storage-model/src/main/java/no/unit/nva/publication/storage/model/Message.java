package no.unit.nva.publication.storage.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.unit.nva.identifiers.SortableIdentifier;
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
    
    private SortableIdentifier identifier;
    private String owner;
    private URI customerId;
    private MessageStatus status;
    private String sender;
    private boolean isDoiRequestRelated;
    private SortableIdentifier resourceIdentifier;
    private String text;
    
    @JacocoGenerated
    public Message() {
    
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
}
