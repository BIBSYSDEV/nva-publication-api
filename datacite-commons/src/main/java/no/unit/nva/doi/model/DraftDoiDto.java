package no.unit.nva.doi.model;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nva.commons.core.JacocoGenerated;

/**
 * Class for exchanging data with DataCite when requesting a draft DOI. The data-structure defined here is used by both
 * request and response to the POST /doi method.
 */
public class DraftDoiDto {

    public static final String DATA_FIELD = "data";
    public static final String ATTRIBUTES_FIELD = "attributes";
    public static final String DOI_FIELD = "doi";
    public static final String PREFIX_FIELD = "prefix";
    public static final String SUFFIX_FIELD = "suffix";
    public static final String TYPE_FIELD = "type";
    public static final String TYPE_FIELD_VALUE = "dois";
    private String doi;
    private String prefix;
    private String suffix;

    @JacocoGenerated
    public DraftDoiDto() {

    }

    /**
     * Create DraftDoiDto containing only the prefix.
     *
     * @param prefix the prefix
     * @return a DraftDoiDto.
     */
    public static DraftDoiDto fromPrefix(String prefix) {
        DraftDoiDto draftDoiDto = new DraftDoiDto();
        draftDoiDto.prefix = prefix;
        return draftDoiDto;
    }

    /**
     * Create a DraftDoiDto from a Json string.
     *
     * @param json a json object as it is expected and retuned from POST /dois endpoint in DataCite.
     * @return a DraftDoiDto.
     */
    public static DraftDoiDto fromJson(String json) {
        JsonNode tree = attempt(() -> dtoObjectMapper.readTree(json)).orElseThrow();
        DraftDoiDto draftDoiDto = new DraftDoiDto();
        JsonNode attributes = tree.path(DATA_FIELD).path(ATTRIBUTES_FIELD);
        draftDoiDto.prefix = attributes.get(PREFIX_FIELD).textValue();
        draftDoiDto.suffix = attributes.get(SUFFIX_FIELD).textValue();
        draftDoiDto.doi = attributes.get(DOI_FIELD).textValue();
        return draftDoiDto;
    }

    public String toJson() {
        ObjectNode rootNode = createJsonObjectWithNestedElements();
        return attempt(() -> dtoObjectMapper.writeValueAsString(rootNode)).orElseThrow();
    }

    @JacocoGenerated
    public String getDoi() {
        return doi;
    }

    @JacocoGenerated
    public String getPrefix() {
        return prefix;
    }

    @JacocoGenerated
    public String getSuffix() {
        return suffix;
    }

    private ObjectNode createJsonObjectWithNestedElements() {
        ObjectNode rootNode = dtoObjectMapper.createObjectNode();
        ObjectNode data = dtoObjectMapper.createObjectNode();
        ObjectNode attributes = dtoObjectMapper.createObjectNode();

        rootNode.set(DATA_FIELD, data);
        rootNode.put(TYPE_FIELD, TYPE_FIELD_VALUE);
        data.set(ATTRIBUTES_FIELD, attributes);
        attributes.put(DOI_FIELD, doi);
        attributes.put(PREFIX_FIELD, prefix);
        attributes.put(SUFFIX_FIELD, suffix);
        return rootNode;
    }
}
