package no.unit.nva.publication.events.bodies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import nva.commons.core.JacocoGenerated;

public class DoiMetadataUpdateEvent {
    
    public static final String REQUEST_DRAFT_DOI_EVENT_TOPIC = "PublicationService.Doi.CreationRequest";
    public static final String UPDATE_DOI_EVENT_TOPIC = "PublicationService.Doi.UpdateRequest";
    protected static final String EMPTY_EVENT_TOPIC = "empty";
    
    
    public static final String TOPIC = "topic";
    public static final String ITEM = "item";
    
    @JsonProperty(TOPIC)
    private final String topic;
    @JsonProperty(ITEM)
    private final Publication item;
    
    @JacocoGenerated
    @JsonCreator
    public DoiMetadataUpdateEvent(
        @JsonProperty(TOPIC) String type,
        @JsonProperty(ITEM) Publication publication) {
        this.topic = type;
        this.item = publication;
    }
    
    public static DoiMetadataUpdateEvent createUpdateDoiEvent(Publication newEntry) {
        return new DoiMetadataUpdateEvent(UPDATE_DOI_EVENT_TOPIC,newEntry);
    }
    
    public static DoiMetadataUpdateEvent createNewDoiEvent(DoiRequest newEntry) {
        return new DoiMetadataUpdateEvent(REQUEST_DRAFT_DOI_EVENT_TOPIC,newEntry.toPublication());
    }
    
    public static DoiMetadataUpdateEvent empty() {
        return new DoiMetadataUpdateEvent(EMPTY_EVENT_TOPIC,null);
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getItem());
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiMetadataUpdateEvent)) {
            return false;
        }
        DoiMetadataUpdateEvent that = (DoiMetadataUpdateEvent) o;
        return Objects.equals(getTopic(), that.getTopic()) && Objects.equals(getItem(), that.getItem());
    }
    
    @JacocoGenerated
    public String getTopic() {
        return topic;
    }
    
    @JacocoGenerated
    public Publication getItem() {
        return item;
    }
}
