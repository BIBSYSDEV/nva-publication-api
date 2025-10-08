package no.sikt.nva.scopus.conversion.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedImportCandidate;

public class ImportCandidateSearchApiResponse implements JsonSerializable {

    public static final String HITS = "hits";
    public static final String TOTAL = "total";
    @JsonProperty(HITS)
    private final List<ExpandedImportCandidate> hits;
    @JsonProperty(TOTAL)
    private final int total;

    @JsonCreator
    public ImportCandidateSearchApiResponse(@JsonProperty(HITS) List<ExpandedImportCandidate> hits,
                                            @JsonProperty(TOTAL) int total) {
        this.hits = hits;
        this.total = total;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }

    public int getTotal() {
        return total;
    }

    public List<ExpandedImportCandidate> getHits() {
        return hits;
    }
}
