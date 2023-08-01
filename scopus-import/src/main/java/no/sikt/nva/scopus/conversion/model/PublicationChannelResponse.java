package no.sikt.nva.scopus.conversion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import lombok.Getter;
import no.unit.nva.commons.json.JsonSerializable;

@Getter
public class PublicationChannelResponse implements JsonSerializable {

    public static final String HITS = "hits";
    public static final String TOTAL_HITS = "totalHits";
    @JsonProperty(TOTAL_HITS)
    private final int totalHits;
    @JsonProperty(HITS)
    private final List<PublicationChannelHit> hits;

    @JsonCreator
    public PublicationChannelResponse(@JsonProperty(TOTAL_HITS) int totalHits,
                                      @JsonProperty(HITS) List<PublicationChannelHit> hits) {
        this.totalHits = totalHits;
        this.hits = hits;
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    @Getter
    public static class PublicationChannelHit implements JsonSerializable{

        public static final String ID = "id";
        @JsonProperty(ID)
        private final URI id;

        @JsonCreator
        public PublicationChannelHit(@JsonProperty(ID) URI id) {
            this.id = id;
        }
    }
}
