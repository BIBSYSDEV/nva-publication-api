package no.unit.nva.model.contexttypes;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ResearchData implements PublicationContext {

    public static final String PUBLISHER_FIELD = "publisher";
    @JsonProperty(PUBLISHER_FIELD)
    private final PublishingHouse publisher;

    public ResearchData(@JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher) {
        this.publisher = publisher;
    }

    public PublishingHouse getPublisher() {
        return isEffectivelyNullPublisher() ? new NullPublisher() : publisher;
    }

    private boolean isEffectivelyNullPublisher() {
        return isNull(publisher) || !publisher.isValid();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResearchData)) {
            return false;
        }
        ResearchData that = (ResearchData) o;
        return Objects.equals(publisher, that.publisher);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(publisher);
    }
}
