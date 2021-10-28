package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.ENVIRONMENT;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.CONTEXT_TYPE_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.JOURNAL_ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.PUBLISHER_ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.SERIES_ID_JSON_PTR;
import static nva.commons.core.StringUtils.isNotBlank;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.expansion.utils.JsonLdUtils;
import no.unit.nva.expansion.utils.UriRetriever;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.GodClass")
public final class IndexDocument implements JsonSerializable, ExpandedResourceUpdate {

    public static final String ID_FIELD_NAME = "id";
    public static final String ID_NAMESPACE = ENVIRONMENT.readEnv("ID_NAMESPACE");
    private static final UriRetriever uriRetriever = new UriRetriever();

    private final JsonNode indexDocumentRootNode;

    public IndexDocument(JsonNode root) {
        this.indexDocumentRootNode = root;
    }

    public static IndexDocument fromPublication(Publication publication) throws JsonProcessingException {
        return fromPublication(uriRetriever, publication);
    }

    public static IndexDocument fromPublication(UriRetriever uriRetriever, Publication publication)
        throws JsonProcessingException {
        var documentWithId = createJsonWithId(publication);
        var enrichedJson = enrichJson(uriRetriever, documentWithId);
        return attempt(() -> objectMapper.readTree(enrichedJson))
            .map(IndexDocument::new)
            .orElseThrow();
    }

    public static List<URI> getPublicationContextUris(JsonNode indexDocument) {
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
        return getPublicationContextUris(indexDocumentRootNode);
    }

    public URI getId() {
        return URI.create(indexDocumentRootNode.at(ID_JSON_PTR).textValue());
    }

    @JacocoGenerated
    @Override
    public String toJsonString() {
        return JsonLdUtils.toJsonString(indexDocumentRootNode);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(indexDocumentRootNode);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexDocument)) {
            return false;
        }
        IndexDocument that = (IndexDocument) o;
        return Objects.equals(indexDocumentRootNode, that.indexDocumentRootNode);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }

    public JsonNode asJsonNode() {
        return indexDocumentRootNode;
    }

    private static String enrichJson(UriRetriever uriRetriever, ObjectNode documentWithId) {
        return attempt(() -> new IndexDocumentWrapperLinkedData(uriRetriever))
            .map(documentWithLinkedData -> documentWithLinkedData.toFramedJsonLd(documentWithId))
            .orElseThrow();
    }

    private static ObjectNode createJsonWithId(Publication publication) throws JsonProcessingException {
        String jsonString = objectMapper.writeValueAsString(publication);
        ObjectNode json = (ObjectNode) objectMapper.readTree(jsonString);
        URI id = new UriWrapper(ID_NAMESPACE).addChild(publication.getIdentifier().toString()).getUri();
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
