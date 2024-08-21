package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.AFFILIATIONS_POINTER;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.CONTEXT_TYPE_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.CONTRIBUTORS_POINTER;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.FUNDING_SOURCE_POINTER;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.INSTANCE_TYPE_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.PUBLICATION_CONTEXT_ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.PUBLISHER_ID_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.SERIES_ID_JSON_PTR;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_HOST_URI;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static nva.commons.core.StringUtils.isNotBlank;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.GodClass")
@JsonTypeName(ExpandedResource.TYPE)
public final class ExpandedResource implements JsonSerializable, ExpandedDataEntry {
    // The ExpandedResource differs from ExpandedDoiRequest and ExpandedMessage
    // because is does not extend the Resource or Publication class,
    // but it contains its data as an inner Json Node.

    public static final String TYPE = "Publication";
    private static final String ID_FIELD_NAME = "id";
    private static final String JSON_LD_CONTEXT_FIELD = "@context";
    private static final String CONTEXT_TYPE_ANTHOLOGY = "Anthology";
    private static final String INSTANCE_TYPE_ACADEMIC_CHAPTER = "AcademicChapter";
    public static final JsonPointer CONTRIBUTORS_PTR = JsonPointer.compile("/entityDescription/contributors");
    public static final String CONTRIBUTOR_SEQUENCE = "sequence";
    public static final String LICENSE_FIELD = "license";
    public static final String ASSOCIATED_ARTIFACTS_FIELD = "associatedArtifacts";
    public static final String TYPE_FIELD = "type";
    @JsonAnySetter
    private final Map<String, Object> allFields;

    public ExpandedResource() {
        this.allFields = new LinkedHashMap<>();
    }

    public static ExpandedResource fromPublication(RawContentRetriever uriRetriever, Publication publication)
        throws JsonProcessingException {
        var documentWithId = transformToJsonLd(publication);
        var enrichedJson = enrichJson(uriRetriever, documentWithId);
        var sortedJson = addFields(enrichedJson, publication);
        return attempt(() -> objectMapper.treeToValue(sortedJson, ExpandedResource.class)).orElseThrow();
    }

    private static JsonNode addFields(String json, Publication publication) {
        var sortedJson = strToJsonWithSortedContributors(json);
        injectHasFileEnum(publication, (ObjectNode) sortedJson);
        expandLicenses(sortedJson);
        return sortedJson;
    }

    private static void expandLicenses(JsonNode node) {
        var optionalAssociatedArtifacts = Optional.ofNullable(node.get(ASSOCIATED_ARTIFACTS_FIELD));
        optionalAssociatedArtifacts.ifPresent(ExpandedResource::handleAssociatedArtifacts);
    }

    private static void handleAssociatedArtifacts(JsonNode associatedArtifacts) {
        if (associatedArtifacts.isArray()) {
            associatedArtifacts.forEach(ExpandedResource::processArtifact);
        }
    }

    private static void processArtifact(JsonNode artifact) {
        if (hasLicense(artifact)) {
            expandLicense(artifact);
        }
    }

    private static void expandLicense(JsonNode artifact) {
        var artifactNode = (ObjectNode) artifact;
        var licenseUri = extractLicenseFromAssociatedArtifactNode(artifact);
        Optional.ofNullable(licenseUri)
            .map(License::fromUri)
            .map(License::toJsonNode)
            .ifPresent(jsonNode -> artifactNode.set(LICENSE_FIELD, jsonNode));
    }

    private static boolean hasLicense(JsonNode artifact) {
        return artifact.has(LICENSE_FIELD);
    }

    private static URI extractLicenseFromAssociatedArtifactNode(JsonNode node) {
        return Optional.ofNullable(node.get(ExpandedResource.LICENSE_FIELD))
                   .map(JsonNode::asText)
                   .map(URI::create)
                   .orElse(null);
    }

    private static void injectHasFileEnum(Publication publication, ObjectNode sortedJson) {
        sortedJson.put(FilesStatus.FILES_STATUS, FilesStatus.fromPublication(publication).getValue());
    }

    private static JsonNode strToJsonWithSortedContributors(String jsonStr) {
        var json = attempt(() -> objectMapper.readTree(jsonStr)).orElseThrow();
        var contributors = json.at(CONTRIBUTORS_PTR);
        if (!contributors.isMissingNode()) {
            var contributorsArray = (ArrayNode) contributors;
            List<JsonNode> contributorsList = new ArrayList<>();
            contributorsArray.forEach(contributorsList::add);
            contributorsList.sort(Comparator.comparingInt(c -> c.get(CONTRIBUTOR_SEQUENCE).asInt()));
            contributorsArray.removeAll();
            contributorsArray.addAll(contributorsList);
        }

        return json;
    }

    public static List<URI> extractPublicationContextUris(JsonNode indexDocument) {
        List<URI> uris = new ArrayList<>();
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

    public static Set<URI> extractAffiliationUris(JsonNode indexDocument) {
        return extractUris(affiliationNodes(indexDocument), "id");
    }

    public static Optional<URI> extractPublicationContextUri(JsonNode indexDocument) {
        return Optional.of(URI.create(indexDocument.at(PUBLICATION_CONTEXT_ID_JSON_PTR).asText()));
    }

    public static boolean isPublicationContextTypeAnthology(JsonNode root) {
        return CONTEXT_TYPE_ANTHOLOGY.equals(getPublicationContextType(root));
    }

    public static boolean isAcademicChapter(JsonNode root) {
        return INSTANCE_TYPE_ACADEMIC_CHAPTER.equals(getInstanceType(root));
    }

    public static ArrayNode fundingNodes(JsonNode root) {
        return (ArrayNode) root.at(FUNDING_SOURCE_POINTER);
    }

    public static Set<URI> extractUris(ArrayNode root, String nodeName) {
        return root.findValues(nodeName).stream()
                   .map(JsonNode::textValue)
                   .map(URI::create)
                   .collect(Collectors.toSet());
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
        if (!(o instanceof ExpandedResource that)) {
            return false;
        }
        //Comparison can only happen when comparing them as json nodes.
        return Objects.equals(this.asJsonNode(), that.asJsonNode());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }

    private static ArrayNode affiliationNodes(JsonNode indexDocument) {
        var affiliationNodes = getJsonNodeStream(indexDocument, CONTRIBUTORS_POINTER)
                                   .flatMap(ExpandedResource::extractAffiliations)
                                   .toList();
        return new ArrayNode(JsonNodeFactory.instance, affiliationNodes);
    }

    private static Stream<JsonNode> extractAffiliations(JsonNode contributorNode) {
        return getJsonNodeStream(contributorNode, AFFILIATIONS_POINTER);
    }

    private static Stream<JsonNode> getJsonNodeStream(JsonNode jsonNode, String jsonPtr) {
        return StreamSupport.stream(jsonNode.at(jsonPtr).spliterator(), false);
    }

    private static String getInstanceType(JsonNode root) {
        return root.at(INSTANCE_TYPE_JSON_PTR).asText();
    }

    private static String enrichJson(RawContentRetriever uriRetriever, ObjectNode documentWithId) {
        return attempt(() -> new IndexDocumentWrapperLinkedData(uriRetriever))
                   .map(documentWithLinkedData -> documentWithLinkedData.toFramedJsonLd(documentWithId))
                   .orElseThrow();
    }

    private static ObjectNode transformToJsonLd(Publication publication) throws JsonProcessingException {
        var jsonString = objectMapper.writeValueAsString(publication);
        var json = (ObjectNode) objectMapper.readTree(jsonString);
        json.put(ID_FIELD_NAME, extractJsonLdId(publication).toString());
        json.set(JSON_LD_CONTEXT_FIELD, extractJsonLdContext(publication));
        return json;
    }

    private static URI extractJsonLdId(Publication publication) {
        return UriWrapper.fromUri(PUBLICATION_HOST_URI).addChild(publication.getIdentifier().toString()).getUri();
    }

    private static JsonNode extractJsonLdContext(Publication publication) {
        var jsonContext = publication.getJsonLdContext();
        return attempt(() -> dtoObjectMapper.readTree(jsonContext)).orElseThrow();
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
        return root.at(PUBLICATION_CONTEXT_ID_JSON_PTR).textValue();
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
