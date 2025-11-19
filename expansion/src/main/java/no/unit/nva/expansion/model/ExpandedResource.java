package no.unit.nva.expansion.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.AFFILIATIONS_POINTER;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.CONTEXT_TYPE_JSON_PTR;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.CONTRIBUTORS_POINTER;
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
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.expansion.ExpansionConfig;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
@JsonTypeName(ExpandedResource.TYPE)
public final class ExpandedResource implements JsonSerializable, ExpandedDataEntry {
    // The ExpandedResource differs from ExpandedDoiRequest and ExpandedMessage
    // because it does not extend the Resource or Publication class,
    // but it contains its data as an inner Json Node.

    public static final String TYPE = "Publication";
    public static final JsonPointer ENTITY_DESCRIPTION_PTR = JsonPointer.compile("/entityDescription");
    public static final JsonPointer CONTRIBUTORS_PTR = JsonPointer.compile("/entityDescription/contributors");
    public static final String CONTRIBUTOR_SEQUENCE = "sequence";
    public static final String LICENSE_FIELD = "license";
    public static final String ASSOCIATED_ARTIFACTS_FIELD = "associatedArtifacts";

    // The join field is used by the search index to create a relationship between parent and child documents.
    private static final String JOIN_FIELD_PARENT_LABEL = "hasParts";
    private static final String JOIN_FIELD_CHILD_LABEL = "partOf";
    private static final String JOIN_FIELD_NODE_LABEL = "joinField";
    private static final String JOIN_FIELD_RELATION_KEY = "name";
    private static final String JOIN_FIELD_PARENT_KEY = "parent";
    public static final String JOIN_FIELD_DUMMY_PARENT_IDENTIFIER = "PARENT_IDENTIFIER_NOT_FOUND";

    private static final String ID_FIELD_NAME = "id";
    private static final String JSON_LD_CONTEXT_FIELD = "@context";
    private static final String CONTEXT_TYPE_ANTHOLOGY = "Anthology";
    private static final String INSTANCE_TYPE_ACADEMIC_CHAPTER = "AcademicChapter";
    public static final int MAX_CONTRIBUTORS_PREVIEW = 10;
    public static final String CONTRIBUTORS_COUNT = "contributorsCount";
    public static final String CONTRIBUTORS_PREVIEW = "contributorsPreview";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandedResource.class);
    public static final String CHILD_PUBLICATIONS = "childPublications";

    @JsonAnySetter
    private final Map<String, Object> allFields;

    public ExpandedResource() {
        this.allFields = new LinkedHashMap<>();
    }

    public static ExpandedResource fromPublication(RawContentRetriever uriRetriever,
                                                   ResourceService resourceService, QueueClient queueClient,
                                                   Resource resource)
        throws JsonProcessingException {
        var documentWithId = transformToJsonLd(resource.toPublication());
        var enrichedJson = enrichJson(uriRetriever, resourceService, queueClient, documentWithId);
        var jsonWithAddedFields = addFields(enrichedJson, resource);
        try {
            return objectMapper.treeToValue(jsonWithAddedFields, ExpandedResource.class);
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to parse expanded resource from JSON: {}", jsonWithAddedFields);
            throw exception;
        }
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
        var contextId = indexDocument.at(PUBLICATION_CONTEXT_ID_JSON_PTR);
        return !contextId.isMissingNode() && !contextId.textValue().isBlank()
                   ? Optional.of(URI.create(contextId.textValue()))
                   : Optional.empty();
    }

    public static boolean isPublicationContextTypeAnthology(JsonNode root) {
        return CONTEXT_TYPE_ANTHOLOGY.equals(getPublicationContextType(root));
    }

    public static boolean isAcademicChapter(JsonNode root) {
        return INSTANCE_TYPE_ACADEMIC_CHAPTER.equals(getInstanceType(root));
    }

    public static Set<URI> extractUris(ArrayNode root, String nodeName) {
        return root.findValues(nodeName).stream().map(JsonNode::textValue).map(URI::create).collect(Collectors.toSet());
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

    private static ObjectNode addFields(String jsonString, Resource resource) {
        var publication = resource.toPublication();
        var objectNode = strToJson(jsonString);
        sortContributors(objectNode);
        injectHasFileEnum(publication, objectNode);
        expandLicenses(objectNode);
        injectJoinField(publication, objectNode);
        injectContributorCount(objectNode);
        injectContributorsPreview(objectNode);
        objectNode.putPOJO(CHILD_PUBLICATIONS, resource.getRelatedResources());
        return objectNode;
    }

    private static void injectContributorCount(ObjectNode json) {
        var contributors = json.at(CONTRIBUTORS_PTR);
        if (!contributors.isMissingNode() && contributors.isArray()) {
            var entityDescription = (ObjectNode) json.at(ENTITY_DESCRIPTION_PTR);
            if (!entityDescription.isMissingNode() && entityDescription.isObject()) {
                entityDescription.put(CONTRIBUTORS_COUNT, contributors.size());
            }
        }
    }

    private static void injectContributorsPreview(ObjectNode json) {
        var contributors = json.at(CONTRIBUTORS_PTR);
        if (!contributors.isMissingNode() && contributors.isArray()) {
            var entityDescription = (ObjectNode) json.at(ENTITY_DESCRIPTION_PTR);
            if (!entityDescription.isMissingNode() && entityDescription.isObject()) {
                var sortedContributors = sortBySequenceAndLimit(contributors);
                var sortedContributorsArrayNode = new ArrayNode(JsonNodeFactory.instance).addAll(sortedContributors);

                entityDescription.set(CONTRIBUTORS_PREVIEW, sortedContributorsArrayNode);
            }
        }
    }

    private static List<JsonNode> sortBySequenceAndLimit(JsonNode contributors) {
        var contributorsList = new ArrayList<JsonNode>();
        contributors.forEach(contributorsList::add);
        return contributorsList.stream()
                   .sorted(Comparator.comparingInt(contributor -> contributor.get(CONTRIBUTOR_SEQUENCE).asInt()))
                   .limit(MAX_CONTRIBUTORS_PREVIEW).toList();
    }

    /**
     * Injects a join field into the JSON document, intended to be used by OpenSearch. The join
     * field is used to create a relationship between parent and child documents, where all
     * documents are considered to either be a potential parent or a child depending on type.
     * "Children" containing a reference to a parent will have this reference in the join field.
     * Note that non-publication contexts (e.g. journals) are not handled and the join field will
     * have an invalid/dummy parent identifier.
     */
    private static void injectJoinField(Publication publication, ObjectNode sortedJson) {
        Optional.ofNullable(publication.getEntityDescription())
                .map(EntityDescription::getReference)
                .ifPresent(reference -> addJoinField(sortedJson, reference));
    }

    private static void addJoinField(ObjectNode sortedJson, Reference reference) {
        var publicationContext = reference.getPublicationContext();
        if (canBeParent(publicationContext)) {
            addJoinField(sortedJson, JOIN_FIELD_PARENT_LABEL, null);
        } else {
            var parentIdentifier = getParentIdentifier(sortedJson);
            addJoinField(sortedJson, JOIN_FIELD_CHILD_LABEL, parentIdentifier);
        }
    }

    private static String getParentIdentifier(ObjectNode sortedJson) {
        return extractPublicationContextUri(sortedJson)
                .filter(uri -> isNotBlank(uri.toString()))
                .map(ExpandedResource::publicationUriToIdentifier)
                .orElse(JOIN_FIELD_DUMMY_PARENT_IDENTIFIER);
    }

    /**
     * Extracts the publication identifier (last segment) from a URI, assuming it is a publication
     * URI. Note that other URIs (e.g. journals) will not be handled and will return null.
     */
    private static String publicationUriToIdentifier(URI uri) {
        return attempt(() -> SortableIdentifier.fromUri(uri))
                .map(SortableIdentifier::toString)
                .orElse(failure -> null);
    }

    private static boolean canBeParent(PublicationContext context) {
        return context instanceof Book;
    }

    private static void addJoinField(ObjectNode sortedJson, String name, String parent) {
        var newNode = sortedJson.putObject(JOIN_FIELD_NODE_LABEL);
        newNode.put(JOIN_FIELD_RELATION_KEY, name);
        if (nonNull(parent)) {
            newNode.put(JOIN_FIELD_PARENT_KEY, parent);
        }
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
        sortedJson.put(FilesStatus.FILES_STATUS, FilesStatus.fromAssociatedArtifacts(publication.getAssociatedArtifacts()).getValue());
    }

    private static ObjectNode strToJson(String jsonStr) {
        return (ObjectNode) attempt(() -> objectMapper.readTree(jsonStr)).orElseThrow();
    }

    private static JsonNode sortContributors(JsonNode json) {
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

    private static ArrayNode affiliationNodes(JsonNode indexDocument) {
        var affiliationNodes = getJsonNodeStream(indexDocument,
                                                 CONTRIBUTORS_POINTER).flatMap(ExpandedResource::extractAffiliations)
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

    private static String enrichJson(RawContentRetriever uriRetriever, ResourceService resourceService,
                                     QueueClient queueClient, ObjectNode documentWithId) {
        return attempt(() -> new IndexDocumentWrapperLinkedData(uriRetriever, resourceService, queueClient))
                   .map(documentWithLinkedData -> documentWithLinkedData.toFramedJsonLd(documentWithId))
                   .orElseThrow();
    }

    private static ObjectNode transformToJsonLd(Publication publication) throws JsonProcessingException {
        var jsonString = objectMapper.writeValueAsString(publication);
        var json = (ObjectNode) objectMapper.readTree(jsonString);
        json.put(ID_FIELD_NAME, extractJsonLdId(publication).toString());
        json.set(JSON_LD_CONTEXT_FIELD, extractJsonLdContext());
        return json;
    }

    private static URI extractJsonLdId(Publication publication) {
        return UriWrapper.fromUri(PUBLICATION_HOST_URI).addChild(publication.getIdentifier().toString()).getUri();
    }

    private static JsonNode extractJsonLdContext() {
        var jsonContext = Publication.getJsonLdContext(ExpansionConfig.getApiHost());
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
