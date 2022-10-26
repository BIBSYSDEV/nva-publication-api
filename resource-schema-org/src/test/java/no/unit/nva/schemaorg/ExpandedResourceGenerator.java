package no.unit.nva.schemaorg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedResource;

import java.nio.file.Path;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;

public class ExpandedResourceGenerator {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

    public static ExpandedResource generateJournalArticle() throws JsonProcessingException {
        var document = stringFromResources(Path.of("01838928cb1a-e8ccea7c-5aa9-4c0a-b7be-314efda0e953"));
        var jsonNode = MAPPER.readTree(document);
        var expandedResourceString = MAPPER.writeValueAsString(jsonNode.get("body"));
        return MAPPER.readValue(expandedResourceString, ExpandedResource.class);

    }
}
