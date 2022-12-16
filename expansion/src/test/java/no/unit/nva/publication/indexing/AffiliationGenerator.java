package no.unit.nva.publication.indexing;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AffiliationGenerator {

    public static final String TOP_LEVEL_SAMPLE_JSON_FILENAME = "framed-json/affiliation_parent_sample.json";
    public static final String SAMPLE_JSON_FILENAME = "framed-json/affiliation_sample.json";
    public static final String FIELD_ID = "id";
    public static final String FIELD_HAS_PART = "/hasPart";
    public static final String FIELD_FIRST_PART_OF = "/partOf/0";

    public static Map<URI, String> getAffiliationsWithCommonParent(List<URI> childUris, URI parentURI) {
        Map<URI, String> uriToContent = new java.util.HashMap<>(Collections.emptyMap());

        String parentSample = stringFromResources(Path.of(TOP_LEVEL_SAMPLE_JSON_FILENAME));
        JsonNode parentJson = attempt(() -> objectMapper.readTree(parentSample)).orElseThrow();

        int i = 0;
        for (URI childURI : childUris) {
            JsonNode content = updateFieldsForChilAffiliation(parentURI, childURI);

            uriToContent.put(childURI, content.toString());

            setChildrenForParentAffiliation(parentJson, i, childURI);

            i++;
        }

        ((ObjectNode) parentJson).put(FIELD_ID, parentURI.toString());

        uriToContent.put(parentURI, parentJson.toString());

        return uriToContent;
    }

    private static void setChildrenForParentAffiliation(JsonNode parentJson, int i, URI childURI) {

        var hasPartPointer = String.format(FIELD_HAS_PART + "/%d", i);
        var hasPartNode = parentJson.at(hasPartPointer);

        if (i < parentJson.at(FIELD_HAS_PART).size()) {
            ((ObjectNode) hasPartNode).put(FIELD_ID, childURI.toString());
        }
    }

    private static JsonNode updateFieldsForChilAffiliation(URI parentURI, URI childURI) {
        String affiliationSample = stringFromResources(Path.of(SAMPLE_JSON_FILENAME));
        JsonNode content = attempt(() -> objectMapper.readTree(affiliationSample)).orElseThrow();

        ((ObjectNode) content).put(FIELD_ID, childURI.toString());

        var firstPartOfNode = content.at(FIELD_FIRST_PART_OF);
        ((ObjectNode) firstPartOfNode).put(FIELD_ID, parentURI.toString());


        return content;
    }
}
