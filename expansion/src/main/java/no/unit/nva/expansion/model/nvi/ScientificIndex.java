package no.unit.nva.expansion.model.nvi;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeName(ScientificIndex.TYPE)
@JsonTypeInfo(use = Id.NAME, property = "type")
public record ScientificIndex(String year, String status) implements JsonSerializable {

    public static final String TYPE = "ScientificIndex";
    public static final String SCIENTIFIC_INDEX_FIELD = "scientificIndex";
    private static final String REPORTED = "reported";

    public JsonNode toJsonNode() {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(this.toJsonString())).orElse(failure -> null);
    }

    @Override
    public String status() {
        return nonNull(status) ? capitalize(status) : null;
    }

    @JsonIgnore
    public boolean isReported() {
        return nonNull(status) && REPORTED.equalsIgnoreCase(status);
    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
