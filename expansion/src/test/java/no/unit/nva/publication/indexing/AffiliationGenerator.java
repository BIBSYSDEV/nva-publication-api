package no.unit.nva.publication.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

public class AffiliationGenerator {

    public static final String TOP_LEVEL_TEMPLATE = "framed-json/affiliation_top.json";
    public static final String MIDDLE_LEVEL_TEMPLATE = "framed-json/affiliation_middle.json";
    public static final String BOTTOM_LEVEL_TEMPLATE = "framed-json/affiliation_bottom_template.json";
    public static final String ID_FIELD = "id";
    public static final String HAS_PART_FIELD = "hasPart";
    public static final String PART_OF_FIELD = "partOf";

    public JsonNode affiliation = readFileAsJson(MIDDLE_LEVEL_TEMPLATE);

    public AffiliationGenerator(URI id, List<URI> hasPartIds, List<URI> partOfIds) {
        affiliation = setId(affiliation, id);

        if (hasPartIds.isEmpty()) {
            ((ObjectNode) affiliation).remove(HAS_PART_FIELD);
        } else {
            setHasPart(hasPartIds);
        }

        if (partOfIds.isEmpty()) {
            ((ObjectNode) affiliation).remove(PART_OF_FIELD);
        } else {
            setPartOf(partOfIds);
        }
    }

    public String getAffiliation() {
        return affiliation.toString();
    }

    private static JsonNode readFileAsJson(String filename) {
        String topLevelSample = stringFromResources(Path.of(filename));
        return attempt(() -> objectMapper.readTree(topLevelSample)).orElseThrow();
    }

    private static JsonNode setId(JsonNode json, URI uri) {
        return ((ObjectNode) json).put(ID_FIELD, uri.toString());
    }

    private void setHasPart(List<URI> hasPartIds) {
        JsonNode childNode = readFileAsJson(BOTTOM_LEVEL_TEMPLATE);

        ArrayNode hasPart = objectMapper.createArrayNode();
        hasPartIds.forEach(id -> hasPart.add(setId(childNode, id)));

        ((ObjectNode) affiliation).replace(HAS_PART_FIELD, hasPart);
    }

    private void setPartOf(List<URI> partOfIds) {
        JsonNode childNode = readFileAsJson(TOP_LEVEL_TEMPLATE);

        ArrayNode partOf = objectMapper.createArrayNode();
        partOfIds.forEach(id -> partOf.add(setId(childNode, id)));

        ((ObjectNode) affiliation).replace(PART_OF_FIELD, partOf);
    }
}
