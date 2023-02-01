package no.unit.nva.publication.indexing;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import no.unit.nva.expansion.utils.UriRetriever;

public class AffiliationGenerator {

    public static final String TOP_LEVEL_TEMPLATE = "framed-json/affiliation_top.json";
    public static final String MIDDLE_LEVEL_TEMPLATE = "framed-json/affiliation_middle.json";
    public static final String BOTTOM_LEVEL_TEMPLATE = "framed-json/affiliation_bottom_template.json";
    public static final String ID_FIELD = "id";
    public static final String HAS_PART_FIELD = "hasPart";
    public static final String PART_OF_FIELD = "partOf";
    private final int depth;
    private final UriRetriever mockUriRetriever;

    public AffiliationGenerator(int depth, UriRetriever mockUriRetriever) {
        this.depth = depth;
        this.mockUriRetriever = mockUriRetriever;
    }

    public URI setAffiliationInMockUriRetriever(URI currentLevelUri) {
        var upperLevelId = List.of(randomUri());
        List<URI> lowerLevelId = List.of();

        if (depth == 0) {
            throw new IllegalStateException("Having no depth makes no sense");
        }

        for (int i = 1; i < depth; i++) {

            setOneAffiliation(currentLevelUri, lowerLevelId, upperLevelId);

            lowerLevelId = List.of(currentLevelUri);
            currentLevelUri = upperLevelId.get(0);
            upperLevelId = List.of(randomUri());
        }

        setOneAffiliation(currentLevelUri, lowerLevelId, List.of());

        return currentLevelUri;
    }

    private void setOneAffiliation(URI uri, List<URI> hasPart, List<URI> partOf) {
        JsonNode affiliation = readFileAsJson(MIDDLE_LEVEL_TEMPLATE);
        setId(affiliation, uri);
        setHasPart(affiliation, hasPart);
        setPartOf(affiliation, partOf);
        mockExpansionOfAffiliations(uri, affiliation);
    }

    private void mockExpansionOfAffiliations(URI uri, JsonNode content) {
        when(mockUriRetriever.getRawContent(eq(uri), any()))
            .thenReturn(Optional.of(content.toString()));
    }

    private static JsonNode readFileAsJson(String filename) {
        String topLevelSample = stringFromResources(Path.of(filename));
        return attempt(() -> objectMapper.readTree(topLevelSample)).orElseThrow();
    }

    private static JsonNode setId(JsonNode json, URI uri) {
        return ((ObjectNode) json).put(ID_FIELD, uri.toString());
    }

    private void setHasPart(JsonNode affiliation, List<URI> hasPartIds) {
        if (hasPartIds.isEmpty()) {
            ((ObjectNode) affiliation).remove(HAS_PART_FIELD);
            return;
        }

        JsonNode childNode = readFileAsJson(BOTTOM_LEVEL_TEMPLATE);

        ArrayNode hasPart = objectMapper.createArrayNode();
        hasPartIds.forEach(id -> hasPart.add(setId(childNode, id)));

        ((ObjectNode) affiliation).replace(HAS_PART_FIELD, hasPart);
    }

    private void setPartOf(JsonNode affiliation, List<URI> partOfIds) {
        if (partOfIds.isEmpty()) {
            ((ObjectNode) affiliation).remove(HAS_PART_FIELD);
            return;
        }

        JsonNode childNode = readFileAsJson(TOP_LEVEL_TEMPLATE);

        ArrayNode partOf = objectMapper.createArrayNode();
        partOfIds.forEach(id -> partOf.add(setId(childNode, id)));

        ((ObjectNode) affiliation).replace(PART_OF_FIELD, partOf);
    }
}
