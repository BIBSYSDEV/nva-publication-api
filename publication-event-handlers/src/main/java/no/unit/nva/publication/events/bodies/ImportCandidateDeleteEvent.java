package no.unit.nva.publication.events.bodies;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

public class ImportCandidateDeleteEvent implements JsonSerializable {

    public static final String EVENT_TOPIC = "ImportCandidates.Scopus.Delete";
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";
    public static final String TOPIC = "topic";
    @JsonProperty(SCOPUS_IDENTIFIER)
    private final String scopusIdentifier;
    @JsonProperty(TOPIC)
    private final String topic;

    @JsonCreator
    public ImportCandidateDeleteEvent(@JsonProperty(TOPIC) String topic,
                                      @JsonProperty(SCOPUS_IDENTIFIER) String scopusIdentifier) {
        this.topic = topic;
        this.scopusIdentifier = scopusIdentifier;
    }

    public static ImportCandidateDeleteEvent fromJson(String json) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, ImportCandidateDeleteEvent.class)).orElseThrow();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getScopusIdentifier(), getTopic());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImportCandidateDeleteEvent that = (ImportCandidateDeleteEvent) o;
        return Objects.equals(getScopusIdentifier(), that.getScopusIdentifier()) && Objects.equals(
            getTopic(), that.getTopic());
    }

    public String getTopic() {
        return topic;
    }

    public String getScopusIdentifier() {
        return scopusIdentifier;
    }
}
