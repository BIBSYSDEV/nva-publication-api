package no.unit.nva.schemaorg;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedResource;

import java.nio.file.Path;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

public class ExpandedResourceGenerator {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
    public static final String BODY_FIELD = "body";
    public static final String JOURNAL_ARTICLE = "01838928cb1a-e8ccea7c-5aa9-4c0a-b7be-314efda0e953";
    public static final String NON_JOURNAL_ARTICLE = "0183c63ba065-eb063976-8d4b-4fcf-87ba-ecbed1721eba";

    public static ExpandedResource generateJournalArticle() {
        var document = stringFromResources(Path.of(JOURNAL_ARTICLE));
        return createExpandedResource(document);

    }

    private static ExpandedResource createExpandedResource(String document) {
        var jsonNode = attempt(() -> MAPPER.readTree(document)).orElseThrow();
        var expandedResourceString = attempt(() -> MAPPER.writeValueAsString(jsonNode.get(BODY_FIELD))).orElseThrow();
        return attempt(() -> MAPPER.readValue(expandedResourceString, ExpandedResource.class)).orElseThrow();
    }

    public static ExpandedResource generateAnyNonJournalArticleType() {
        var document = stringFromResources(Path.of(NON_JOURNAL_ARTICLE));
        return createExpandedResource(document);
    }
}
