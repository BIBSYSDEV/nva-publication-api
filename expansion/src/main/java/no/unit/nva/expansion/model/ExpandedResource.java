package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.CONTEXT_TYPE_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.JOURNAL_ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.PUBLISHER_ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.SERIES_ID_JSON_PTR;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_HOST_URI;
import static nva.commons.core.StringUtils.isNotBlank;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.expansion.utils.UriRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.GodClass")
@JsonTypeName(ExpandedResource.TYPE)
public final class ExpandedResource implements JsonSerializable, ExpandedDataEntry {
    
    // The ExpandedResource differs from ExpandedDoiRequest and ExpandedMessage
    // because is does not extend the Resource or Publication class,
    // but it contains its data as an inner Json Node.
    public static final String ID_FIELD_NAME = "id";
    public static final String TYPE = "Publication";
    private static final UriRetriever uriRetriever = new UriRetriever();
    @JsonAnySetter
    private final Map<String, Object> allFields;
    
    public ExpandedResource() {
        this.allFields = new LinkedHashMap<>();
    }
    
    public static ExpandedResource fromPublication(Publication publication) throws JsonProcessingException {
        return fromPublication(uriRetriever, publication);
    }
    
    public static ExpandedResource fromPublication(UriRetriever uriRetriever, Publication publication)
        throws JsonProcessingException {
        var documentWithId = createJsonWithId(publication);
        var enrichedJson = enrichJson(uriRetriever, documentWithId);
        return attempt(() -> objectMapper.readValue(enrichedJson, ExpandedResource.class)).orElseThrow();
    }
    
    public static List<URI> extractPublicationContextUris(JsonNode indexDocument) {
        List<URI> uris = new java.util.ArrayList<>();
        if (isJournal(indexDocument) && isPublicationChannelId(getJournalIdStr(indexDocument))) {
            uris.add(getJournalURI(indexDocument));
        }
        if (hasPublisher(indexDocument)) {
            uris.add(getPublisherUri(indexDocument));
        }
        if (hasPublicationChannelBookSeriesId(indexDocument)) {
            uris.add(getBookSeriesUri(indexDocument));
        }
        return uris;
    }
    
    public List<URI> getPublicationContextUris() {
        ObjectNode docAsObjectNode = objectMapper.convertValue(this.allFields, ObjectNode.class);
        return extractPublicationContextUris(docAsObjectNode);
    }
    
    @JacocoGenerated
    @JsonAnyGetter
    public Map<String, Object> getAllFields() {
        return this.allFields;
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return SortableIdentifier.fromUri(fetchId());
    }
    
    public URI fetchId() {
        return URI.create(objectMapper.convertValue(this.allFields, ObjectNode.class).at(ID_JSON_PTR).textValue());
    }
    
    @JacocoGenerated
    @Override
    public String toJsonString() {
        return attempt(() -> objectMapper.writeValueAsString(this)).orElseThrow();
    }
    
    public ObjectNode asJsonNode() {
        return objectMapper.convertValue(this, ObjectNode.class);
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(this.asJsonNode());
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpandedResource)) {
            return false;
        }
        ExpandedResource that = (ExpandedResource) o;
        //Comparison can only happen when comparing them as json nodes.
        return Objects.equals(this.asJsonNode(), that.asJsonNode());
    }
    
    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }
    
    private static String enrichJson(UriRetriever uriRetriever, ObjectNode documentWithId) {
        return attempt(() -> new IndexDocumentWrapperLinkedData(uriRetriever))
                   .map(documentWithLinkedData -> documentWithLinkedData.toFramedJsonLd(documentWithId))
                   .orElseThrow();
    }
    
    private static ObjectNode createJsonWithId(Publication publication) throws JsonProcessingException {
        var jsonString = objectMapper.writeValueAsString(publication);
        var json = (ObjectNode) objectMapper.readTree(jsonString);
        var id = UriWrapper.fromUri(PUBLICATION_HOST_URI).addChild(publication.getIdentifier().toString()).getUri();
        json.put(ID_FIELD_NAME, id.toString());
        return json;
    }
    
    private static String getPublicationContextType(JsonNode root) {
        return root.at(CONTEXT_TYPE_JSON_PTR).textValue();
    }
    
    private static boolean isPublicationChannelId(String uriCandidate) {
        return isNotBlank(uriCandidate) && uriCandidate.contains("publication-channels");
    }
    
    private static boolean isJournal(JsonNode root) {
        return "Journal".equals(getPublicationContextType(root));
    }
    
    private static boolean hasPublisher(JsonNode root) {
        return isPublicationChannelId(getPublisherId(root));
    }
    
    private static String getPublisherId(JsonNode root) {
        return root.at(PUBLISHER_ID_JSON_PTR).textValue();
    }
    
    private static URI getPublisherUri(JsonNode root) {
        return URI.create(getPublisherId(root));
    }
    
    private static URI getJournalURI(JsonNode root) {
        return URI.create(getJournalIdStr(root));
    }
    
    private static String getJournalIdStr(JsonNode root) {
        return root.at(JOURNAL_ID_JSON_PTR).textValue();
    }
    
    private static URI getBookSeriesUri(JsonNode root) {
        return URI.create(getBookSeriesUriStr(root));
    }
    
    private static String getBookSeriesUriStr(JsonNode root) {
        return root.at(SERIES_ID_JSON_PTR).textValue();
    }
    
    private static boolean hasPublicationChannelBookSeriesId(JsonNode root) {
        return isPublicationChannelId(getBookSeriesUriStr(root));
    }
}
