package no.unit.nva.publication.events.handlers.persistence;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.persistence.PersistedDocumentConsumptionAttributes.createAttributes;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import nva.commons.core.JacocoGenerated;

public class PersistedDocument implements JsonSerializable {
    
    public static final String BODY = "body";
    public static final String CONSUMPTION_ATTRIBUTES = "consumptionAttributes";
    @JsonProperty(CONSUMPTION_ATTRIBUTES)
    private final PersistedDocumentConsumptionAttributes consumptionAttributes;
    @JsonProperty(BODY)
    private final ExpandedDataEntry body;
    
    @JsonCreator
    public PersistedDocument(
        @JsonProperty(BODY) ExpandedDataEntry body,
        @JsonProperty(CONSUMPTION_ATTRIBUTES) PersistedDocumentConsumptionAttributes consumptionAttributes) {
        
        this.consumptionAttributes = consumptionAttributes;
        this.body = body;
    }
    
    public static PersistedDocument createIndexDocument(ExpandedDataEntry expandedResourceUpdate) {
        return new PersistedDocument(expandedResourceUpdate, createAttributes(expandedResourceUpdate));
    }
    
    public static PersistedDocument fromJsonString(String indexingEventPayload) throws JsonProcessingException {
        return objectMapper.readValue(indexingEventPayload, PersistedDocument.class);
    }
    
    @JacocoGenerated
    public PersistedDocumentConsumptionAttributes getConsumptionAttributes() {
        return consumptionAttributes;
    }
    
    @JacocoGenerated
    public ExpandedDataEntry getBody() {
        return body;
    }
    
    @Override
    public String toJsonString() {
        return attempt(() -> objectMapper.writeValueAsString(this)).orElseThrow();
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getConsumptionAttributes(), getBody());
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
        return Objects.equals(getConsumptionAttributes(), that.getConsumptionAttributes())
               && Objects.equals(getBody(), that.getBody());
    }
}
