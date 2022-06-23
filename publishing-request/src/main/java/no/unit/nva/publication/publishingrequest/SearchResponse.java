package no.unit.nva.publication.publishingrequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.List;

public class SearchResponse<E> implements JsonSerializable {

    @JsonIgnore
    public static final String ERROR_MESSAGE_PAGE_OUT_OF_SCOPE =
        "Page requested is out of scope. Query contains %s results";
    @JsonIgnore
    public static final int FIRST_RECORD_ZERO_WHEN_NO_HITS = 0;

    @JsonProperty("@context")
    private String context;
    @JsonProperty
    private URI id;
    @JsonProperty
    private Integer size;
    @JsonProperty
    private Integer firstRecord;
    @JsonProperty
    private URI nextResults;
    @JsonProperty
    private URI previousResults;
    @JsonProperty
    private List<E> hits;

    private SearchResponse(Builder builder) {
        context = builder.context;
        id = builder.id;
        size = builder.size;
        firstRecord = builder.firstRecord;
        nextResults = builder.nextResults;
        previousResults = builder.previousResults;
        hits = builder.hits;
    }

    public SearchResponse() {
    }

    public String getContext() {
        return context;
    }

    public URI getId() {
        return id;
    }

    public Integer getSize() {
        return size;
    }

    public Integer getFirstRecord() {
        return firstRecord;
    }

    public URI getNextResults() {
        return nextResults;
    }

    public URI getPreviousResults() {
        return previousResults;
    }

    public List<E> getHits() {
        return hits;
    }

    @JacocoGenerated
    public static final class Builder<E> {
        private String context;
        private URI id;
        private Integer size;
        private Integer firstRecord;
        private URI nextResults;
        private URI previousResults;
        private List<E> hits;

        public Builder() {
        }

        public Builder withContext(String val) {
            context = val;
            return this;
        }

        public Builder withId(URI val) {
            id = val;
            return this;
        }

        public Builder withSize(Integer val) {
            size = val;
            return this;
        }

        public Builder withFirstRecord(Integer val) {
            firstRecord = val;
            return this;
        }

        public Builder withNextResults(URI val) {
            nextResults = val;
            return this;
        }

        public Builder withPreviousResults(URI val) {
            previousResults = val;
            return this;
        }

        public Builder withHits(List<E> val) {
            hits = val;
            return this;
        }

        public SearchResponse build() {
            return new SearchResponse(this);
        }
    }
}
