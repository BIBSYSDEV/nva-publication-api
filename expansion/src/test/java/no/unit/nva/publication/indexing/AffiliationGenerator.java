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
import java.util.List;
import java.util.Optional;
import no.unit.nva.expansion.utils.UriRetriever;

public class AffiliationGenerator {


    public static final String AFFILIATION_TEMPLATE = "framed-json/affiliation_template.json";
    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String HAS_PART_FIELD = "hasPart";
    public static final String PART_OF_FIELD = "partOf";
    private static final String CONTEXT = "https://bibsysdev.github.io/src/organization-context.json";
    private static final String CONTEXT_FIELD = "@context";
    private final int depth;
    private final UriRetriever mockUriRetriever;

    public AffiliationGenerator(int depth, UriRetriever mockUriRetriever) {
        this.depth = depth;
        this.mockUriRetriever = mockUriRetriever;
    }

    public URI setAffiliationInMockUriRetriever(URI currentLevelUri) {
        var upperLevelId = List.of(randomUri());
        List<URI> lowerLevelId = List.of();

        for (int i = 0; i < depth - 1; i++) {

            setOneAffiliation(currentLevelUri, lowerLevelId, upperLevelId, i);

            lowerLevelId = List.of(currentLevelUri);
            currentLevelUri = upperLevelId.get(0);
            upperLevelId = List.of(randomUri());
        }

        setOneAffiliation(currentLevelUri, lowerLevelId, List.of(), depth-1);

        return currentLevelUri;
    }

    private void setOneAffiliation(URI uri, List<URI> hasPart, List<URI> partOf, int level) {
        JsonNode affiliation = readFileAsJson();
        setId(affiliation, uri);
        setName(affiliation, level);
        setHasPart(affiliation, hasPart);
        setPartOf(affiliation, partOf);
        setContext(affiliation);
        mockExpansionOfAffiliations(uri, affiliation);
    }

    private void mockExpansionOfAffiliations(URI uri, JsonNode content) {
        when(mockUriRetriever.getRawContent(eq(uri), any()))
            .thenReturn(Optional.of(content.toString()));
    }

    private static JsonNode readFileAsJson() {
        return attempt(() -> objectMapper.readTree(
            stringFromResources(
                Path.of(AffiliationGenerator.AFFILIATION_TEMPLATE)))
        ).orElseThrow();
    }

    private static JsonNode setContext(JsonNode json) {
        return ((ObjectNode) json).put(CONTEXT_FIELD, CONTEXT);
    }

    private static JsonNode setId(JsonNode json, URI uri) {
        return ((ObjectNode) json).put(ID_FIELD, uri.toString());
    }

    private static JsonNode setName(JsonNode json, int level) {
        JsonNode node = attempt(() ->
                objectMapper.readValue("{\"nb\": \"name from level: " + level + "\"}", JsonNode.class)
        ).orElseThrow();
        return ((ObjectNode) json).put("name", node);

    }

    private void setHasPart(JsonNode affiliation, List<URI> hasPartIds) {
        if (hasPartIds.isEmpty()) {
            ((ObjectNode) affiliation).remove(HAS_PART_FIELD);
            return;
        }

        JsonNode hasPartContent = readFileAsJson();

        ArrayNode hasPart = objectMapper.createArrayNode();
        hasPartIds.forEach(id -> hasPart.add(setId(hasPartContent, id)));

        ((ObjectNode) affiliation).set(HAS_PART_FIELD, hasPart);
    }

    private void setPartOf(JsonNode affiliation, List<URI> partOfIds) {
        if (partOfIds.isEmpty()) {
            ((ObjectNode) affiliation).remove(PART_OF_FIELD);
            return;
        }

        JsonNode partOfContent = readFileAsJson();

        ArrayNode partOf = objectMapper.createArrayNode();
        partOfIds.forEach(id -> partOf.add(setId(partOfContent, id)));

        ((ObjectNode) affiliation).set(PART_OF_FIELD, partOf);
    }
}
