package no.sikt.nva.scopus;

import java.net.URI;
import no.unit.nva.events.models.EventReference;

public class NewScopusEntryEvent extends EventReference {

    private static final String EVENT_TOPIC = "FatchDoi.DataImport.Scopus";
    private static final String EVENT_SUBTOPIC = null;

    public NewScopusEntryEvent(URI fileLocation) {
        super(EVENT_TOPIC,EVENT_SUBTOPIC,fileLocation);
    }
}
