package no.unit.nva.publication.indexing;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.indexing.IndexingConfig.indexingMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonLdUtils {

    private JsonLdUtils() {

    }

    public static String toJsonString(JsonNode root) {
        return attempt(() -> indexingMapper.writeValueAsString(addContext(root))).orElseThrow();
    }

    private static JsonNode addContext(JsonNode root) {
        if (nonNull(root)) {
            ObjectNode context = indexingMapper.createObjectNode();
            context.put("@vocab", "https://bibsysdev.github.io/src/nva/ontology.ttl#");
            context.put("id", "@id");
            context.put("type", "@type");
            ObjectNode series = indexingMapper.createObjectNode();
            series.put("@type", "@id");
            context.set("series", series);
            ((ObjectNode) root).set("@context", context);
        }
        return root;
    }
}
