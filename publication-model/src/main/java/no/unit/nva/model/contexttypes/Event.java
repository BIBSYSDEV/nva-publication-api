package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import no.unit.nva.model.Agent;
import no.unit.nva.model.contexttypes.place.Place;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Event implements PublicationContext {
    public static final String LABEL = "label";
    public static final String PLACE = "place";
    public static final String TIME = "time";
    public static final String AGENT = "agent";
    public static final String PRODUCT = "product";
    public static final String SUB_EVENT = "subEvent";
    @JsonProperty(LABEL)
    private final String label;
    @JsonProperty(PLACE)
    private final Place place;
    @JsonProperty(TIME)
    private final Time time;
    @JsonProperty(AGENT)
    private final Agent agent;
    @JsonProperty(PRODUCT)
    private final URI product;
    @JsonProperty(SUB_EVENT)
    private final Event subEvent;

    public Event(@JsonProperty(LABEL) String label,
                 @JsonProperty(PLACE) Place place,
                 @JsonProperty(TIME) Time time,
                 @JsonProperty(AGENT) Agent agent,
                 @JsonProperty(PRODUCT) URI product,
                 @JsonProperty(SUB_EVENT) Event subEvent) {
        this.label = label;
        this.place = place;
        this.time = time;
        this.agent = agent;
        this.product = product;
        this.subEvent = subEvent;
    }

    public String getLabel() {
        return label;
    }

    public Place getPlace() {
        return place;
    }

    public Time getTime() {
        return time;
    }

    public Agent getAgent() {
        return agent;
    }

    public Optional<URI> getProduct() {
        return Optional.ofNullable(product);
    }

    public Optional<Event> getSubEvent() {
        return Optional.ofNullable(subEvent);
    }

    public static final class Builder {
        private String label;
        private Place place;
        private Time time;
        private Agent agent;
        private URI product;
        private Event subEvent;

        public Builder() {
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder withPlace(Place place) {
            this.place = place;
            return this;
        }

        public Builder withTime(Time time) {
            this.time = time;
            return this;
        }

        public Builder withAgent(Agent agent) {
            this.agent = agent;
            return this;
        }

        public Builder withProduct(URI product) {
            this.product = product;
            return this;
        }

        public Builder withSubEvent(Event subEvent) {
            this.subEvent = subEvent;
            return this;
        }

        public Event build() {
            return new Event(label, place, time, agent, product, subEvent);
        }
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event)) {
            return false;
        }
        Event event = (Event) o;
        return Objects.equals(getLabel(), event.getLabel())
                && Objects.equals(getPlace(), event.getPlace())
                && Objects.equals(getTime(), event.getTime())
                && Objects.equals(getAgent(), event.getAgent())
                && Objects.equals(getProduct(), event.getProduct())
                && Objects.equals(getSubEvent(), event.getSubEvent());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getLabel(), getPlace(), getTime(), getAgent(), getProduct(), getSubEvent());
    }
}
