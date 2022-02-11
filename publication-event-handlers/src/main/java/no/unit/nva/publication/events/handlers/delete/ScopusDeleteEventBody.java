package no.unit.nva.publication.events.handlers.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScopusDeleteEventBody {

    public static final String TOPIC = "NvaFetchDoi.Scopus.Delete";
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";
    @JsonProperty(SCOPUS_IDENTIFIER)
    private final String scopusIdentifier;

    @JsonCreator
    public ScopusDeleteEventBody(@JsonProperty(SCOPUS_IDENTIFIER) String scopusIdentifier) {
        this.scopusIdentifier = scopusIdentifier;
    }

    public String getScopusIdentifier() {
        return scopusIdentifier;
    }
}
