package no.unit.nva.cristin.mapper.nva;

import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.Event;

public class NvaEventBuilder {

    CristinObject cristinObject;

    public NvaEventBuilder(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public Event buildEvent() {
        return new Event.Builder().withLabel(extractEventTitle()).build();
    }

    private String extractEventTitle() {
        return cristinObject.getLectureOrPosterMetaData().getCristinEventMetaData().getTitle();
    }
}
