package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import static java.util.Objects.nonNull;
import java.net.URI;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Agent;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.place.Place;

public final class EventMerger extends PublicationContextMerger {

    private EventMerger(Record record) {
        super(record);
    }

    public static Event merge(Event event, PublicationContext publicationContext) {
        if (publicationContext instanceof Event newEvent) {
            return new Event.Builder()
                       .withPlace(getPlace(event.getPlace(), newEvent.getPlace()))
                       .withSubEvent(getSubEvent(event, newEvent))
                       .withProduct(getProduct(event, newEvent))
                       .withTime(getTime(event.getTime(), newEvent.getTime()))
                       .withAgent(getAgent(event.getAgent(), newEvent.getAgent()))
                       .withLabel(getNonNullValue(event.getLabel(), newEvent.getLabel()))
                       .build();
        } else {
            return event;
        }
    }

    private static Agent getAgent(Agent oldAgent, Agent newAgent) {
        return nonNull(oldAgent) ? oldAgent : newAgent;
    }

    private static URI getProduct(Event event, Event newEvent) {
        return event.getProduct().isPresent()
                   ? event.getProduct().orElse(null)
                   : newEvent.getProduct().orElse(null);
    }

    private static Event getSubEvent(Event oldEvent, Event newEvent) {
        return nonNull(oldEvent) ? oldEvent : newEvent;
    }

    private static Place getPlace(Place oldPlace, Place newPlace) {
        return nonNull(oldPlace) ? oldPlace : newPlace;
    }
}
