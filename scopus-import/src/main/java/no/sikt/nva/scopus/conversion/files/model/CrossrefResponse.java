package no.sikt.nva.scopus.conversion.files.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;

/**
 * See https://github.com/CrossRef/rest-api-doc/blob/master/api_format.md#license for documentation.
 */

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
        private final String contentType;
        private final ContentVersion contentVersion;

        @JsonCreator
        public CrossrefLink(@JsonProperty("URL") URI uri, @JsonProperty("content-type") String contentType,
                            @JsonProperty("content-version") ContentVersion contentVersion) {
            this.uri = uri;
            this.contentType = contentType;
            this.contentVersion = contentVersion;
        }

        public ContentVersion getContentVersion() {
            return contentVersion;
        }

        public String getContentType() {
            return contentType;
        }

        public URI getUri() {
            return uri;
        }
    }

    public static class Message implements JsonSerializable {

        private final List<License> license;
        private final List<CrossrefLink> links;
        private final Resource resource;

        @JsonCreator
        public Message(@JsonProperty("link") List<CrossrefLink> links, @JsonProperty("license") List<License> license,
                       @JsonProperty("resource") Resource resource) {
            this.links = links;
            this.license = license;
            this.resource = resource;
        }

        public List<License> getLicense() {
            return license;
        }

        public List<CrossrefLink> getLinks() {
            return links;
        }

        public Resource getResource() {
            return resource;
        }
    }

    public static class License implements JsonSerializable {

        private final URI uri;
        private final int delay;
        private final Start start;
        private final ContentVersion contentVersion;

        @JsonCreator
        public License(@JsonProperty("URL") URI uri, @JsonProperty("delay-in-days") int delay,
                       @JsonProperty("start") Start start,
                       @JsonProperty("content-version") ContentVersion contentVersion) {
            this.uri = uri;
            this.delay = delay;
            this.start = start;
            this.contentVersion = contentVersion;
        }

        public ContentVersion getContentVersion() {
            return contentVersion;
        }

        public Start getStart() {
            return start;
        }

        public int getDelay() {
            return delay;
        }

        public URI getUri() {
            return uri;
        }

        public boolean hasDelay() {
            return delay != 0;
        }

        public boolean hasUnspecifiedContentVersion() {
            return ContentVersion.UNSPECIFIED.equals(contentVersion);
        }
    }

    public static class Start implements JsonSerializable {

        /**
         * List of date parts in format [yyyy, mm, dd] where year only has to be present.
         */
        private final List<List<Integer>> dateParts;

        @JsonCreator
        public Start(@JsonProperty("date-parts") List<List<Integer>> dateParts) {
            this.dateParts = dateParts;
        }

        public List<List<Integer>> getDateParts() {
            return dateParts;
        }

        public Integer getYear() {
            return Optional.ofNullable(dateParts.get(0)).map(list -> list.get(0)).orElse(null);
        }

        public Integer getMonth() {
            return Optional.ofNullable(dateParts.get(0)).map(list -> list.get(1)).orElse(null);
        }

        public Integer getDay() {
            return Optional.ofNullable(dateParts.get(0)).map(list -> list.get(2)).orElse(null);
        }
    }

    public static class Resource implements JsonSerializable {

        private final Primary primary;

        @JsonCreator
        public Resource(@JsonProperty("primary") Primary primary) {
            this.primary = primary;
        }

        public Primary getPrimary() {
            return primary;
        }
    }

    public static class Primary implements JsonSerializable {

        private final URI uri;

        @JsonCreator
        public Primary(@JsonProperty("URL") URI uri) {
            this.uri = uri;
        }

        public URI getUri() {
            return uri;
        }
    }
}
