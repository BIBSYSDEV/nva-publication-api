package no.sikt.nva.scopus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public class CrossrefResponse implements JsonSerializable {

    private final Message message;

    @JsonCreator
    public CrossrefResponse(@JsonProperty("message") Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    public static class CrossrefLink implements JsonSerializable {

        private final URI uri;

        @JsonCreator
        public CrossrefLink(@JsonProperty("URL") URI uri) {
            this.uri = uri;
        }

        public URI getUri() {
            return uri;
        }
    }

    public static class Message implements JsonSerializable {

        private final List<CrossrefLink> links;

        @JsonCreator
        public Message(@JsonProperty("link") List<CrossrefLink> links) {
            this.links = links;
        }

        public List<CrossrefLink> getLinks() {
            return links;
        }
    }
}
