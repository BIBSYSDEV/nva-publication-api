package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    private static final String LABEL_FIELD = "label";
    private static final String NAME_FIELD = "name";
    public static final String PLACE_FIELD = "place";
    public static final String TIME_FIELD = "time";
    public static final String AGENT_FIELD = "agent";
    public static final String PRODUCT_FIELD = "product";
    public static final String SUB_EVENT_FIELD = "subEvent";
    @JsonAlias(LABEL_FIELD)
    @JsonProperty(NAME_FIELD)
    private final String name;
    @JsonProperty(PLACE_FIELD)
    private final Place place;
    @JsonProperty(TIME_FIELD)
    private final Time time;
    @JsonProperty(AGENT_FIELD)
    private final Agent agent;
    @JsonProperty(PRODUCT_FIELD)
    private final URI product;
    @JsonProperty(SUB_EVENT_FIELD)
    private final Event subEvent;

    public Event(@JsonAlias(LABEL_FIELD) @JsonProperty(NAME_FIELD)String name,
                 @JsonProperty(PLACE_FIELD) Place place,
                 @JsonProperty(TIME_FIELD) Time time,
                 @JsonProperty(AGENT_FIELD) Agent agent,
                 @JsonProperty(PRODUCT_FIELD) URI product,
                 @JsonProperty(SUB_EVENT_FIELD) Event subEvent) {
        this.name = name;
        this.place = place;
        this.time = time;
        this.agent = agent;
        this.product = product;
        this.subEvent = subEvent;
    }

    public String getName() {
        return name;
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
        private String name;
        private Place place;
        private Time time;
        private Agent agent;
        private URI product;
        private Event subEvent;

        public Builder() {
        }

        /**
         * This method is deprecated because it conflicts with the generally used "label" concept "labels", which is
         * always and only a language map. Use @{link #withName}.
         * @param label The name of the Event.
         * @return Event.Builder.
         */
        @Deprecated(since = "2024-11-05")
        public Builder withLabel(String label) {
            this.name = label;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
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
            return new Event(name, place, time, agent, product, subEvent);
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
        return Objects.equals(getName(), event.getName())
                && Objects.equals(getPlace(), event.getPlace())
                && Objects.equals(getTime(), event.getTime())
                && Objects.equals(getAgent(), event.getAgent())
                && Objects.equals(getProduct(), event.getProduct())
                && Objects.equals(getSubEvent(), event.getSubEvent());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getPlace(), getTime(), getAgent(), getProduct(), getSubEvent());
    }
}
