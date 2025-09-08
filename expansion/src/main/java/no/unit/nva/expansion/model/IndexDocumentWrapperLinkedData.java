package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.ResourceExpansionServiceImpl.API_HOST;
import static no.unit.nva.expansion.model.ExpandedResource.extractAffiliationUris;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUri;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static no.unit.nva.expansion.model.ExpandedResource.extractUris;
import static no.unit.nva.expansion.model.ExpandedResource.fundingNodes;
import static no.unit.nva.expansion.model.ExpandedResource.isAcademicChapter;
import static no.unit.nva.expansion.model.ExpandedResource.isPublicationContextTypeAnthology;
import static no.unit.nva.expansion.utils.JsonLdUtils.toJsonString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.nvi.NviCandidateResponse;
import no.unit.nva.expansion.model.nvi.ScientificIndex;
import no.unit.nva.expansion.utils.FramedJsonGenerator;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.RecoveryEntry;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexDocumentWrapperLinkedData {

    private static final Logger logger = LoggerFactory.getLogger(IndexDocumentWrapperLinkedData.class);
    private static final String SOURCE = "source";
    private static final String FRAME_JSON = "frame.json";
    private static final String CONTEXT = "@context";
    @Deprecated
    private static final String contextAsString =
        "{\n"
        + "  \"@vocab\": \"https://nva.sikt.no/ontology/publication#\",\n"
        + "  \"id\": \"@id\",\n"
        + "  \"type\": \"@type\",\n"
        + "  \"name\": {\n"
        + "    \"@id\": \"label\",\n"
        + "    \"@container\": \"@language\"\n"
        + "  }\n"
        + "}\n";
    private static final JsonNode CONTEXT_NODE = attempt(() -> objectMapper.readTree(contextAsString)).get();
    private static final String FETCHING_NVI_CANDIDATE_ERROR_MESSAGE =
        "Could not fetch nvi candidate for publication with identifier: %s";
    private static final String EXCEPTION = "Exception {}:";
    private static final String ID = "id";
    private static final String SCIENTIFIC_INDEX = "scientific-index";
    private static final String PUBLICATION = "publication";
    private static final int ONE_HUNDRED = 100;
    private static final int SUCCESS_FAMILY = 2;
    private static final int CLIENT_ERROR_FAMILY = 4;
    private static final String TYPE = "type";
    private static final String REPORT_STATUS = "report-status";
    private final RawContentRetriever uriRetriever;
    private final ResourceService resourceService;
    private final QueueClient queueClient;

    public IndexDocumentWrapperLinkedData(RawContentRetriever uriRetriever, ResourceService resourceService,
                                          QueueClient queueClient) {
        this.uriRetriever = uriRetriever;
        this.resourceService = resourceService;
        this.queueClient = queueClient;
    }

    public String toFramedJsonLd(JsonNode indexDocument) {
        var frame = SearchIndexFrame.getFrameWithContext(Path.of(FRAME_JSON));
        var inputStreams = getInputStreams(indexDocument);
        return new FramedJsonGenerator(inputStreams, frame).getFramedJson();
    }

    //TODO: parallelize

    private static URI fetchNviCandidateUri(String publicationId) {
        var urlEncodedPublicationId = URLEncoder.encode(publicationId, StandardCharsets.UTF_8);
        var uri = UriWrapper.fromHost(API_HOST)
                      .addChild(SCIENTIFIC_INDEX)
                      .addChild(PUBLICATION)
                      .getUri();
        return URI.create(String.format("%s/%s/%s", uri, urlEncodedPublicationId, REPORT_STATUS));
    }

    private List<InputStream> getInputStreams(JsonNode indexDocument) {
        final List<InputStream> inputStreams = new ArrayList<>();
        injectScientificIndexStatus(indexDocument);
        inputStreams.add(stringToStream(toJsonString(indexDocument)));
        fetchAnthologyContent(indexDocument).ifPresent(inputStreams::add);
        inputStreams.addAll(fetchAll(extractAffiliationUris(indexDocument), indexDocument));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(indexDocument), indexDocument));
        inputStreams.addAll(fetchFundingSourcesAddingContext(indexDocument));
        inputStreams.removeIf(Objects::isNull);
        return inputStreams;
    }

    private void injectScientificIndexStatus(JsonNode indexDocument) {
        ((ObjectNode) indexDocument).set(ScientificIndex.SCIENTIFIC_INDEX_FIELD, fetchNviStatus(indexDocument));
    }

    private JsonNode fetchNviStatus(JsonNode indexDocument) {
        var publicationId = getId(indexDocument);
        try {
            return fetchNviCandidate(publicationId)
                       .map(this::processNviCandidateResponse)
                       .orElseThrow();
        } catch (Exception e) {
            logger.error(EXCEPTION, e.toString());
            throw ExpansionException.withMessage(String.format(FETCHING_NVI_CANDIDATE_ERROR_MESSAGE, publicationId));
        }
    }

    private static String getId(JsonNode indexDocument) {
        return indexDocument.get(ID).asText();
    }

    private JsonNode processNviCandidateResponse(HttpResponse<String> response) {
        if (response.statusCode() / ONE_HUNDRED == SUCCESS_FAMILY) {
            var nviStatus = toNviCandidateResponse(response.body()).toNviStatus();
            return nviStatus.isReported() ? nviStatus.toJsonNode() : new ObjectNode(null);
        } else if (response.statusCode() == SC_NOT_FOUND) {
            return new ObjectNode(null);
        } else {
            throw new RuntimeException("Unexpected response " + response);
        }
    }

    private Optional<HttpResponse<String>> fetchNviCandidate(String publicationId) {
        return attempt(() -> uriRetriever.fetchResponse(fetchNviCandidateUri(publicationId), "application/json"))
                   .orElseThrow();
    }

    private NviCandidateResponse toNviCandidateResponse(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, NviCandidateResponse.class)).orElseThrow();
    }

    @Deprecated
    private Collection<? extends InputStream> fetchFundingSourcesAddingContext(JsonNode indexDocument) {
        return fetchFundingSources(indexDocument).stream()
                   .map(IoUtils::stringToStream)
                   .map(this::addPotentiallyMissingContext)
                   .toList();
    }

    private Collection<String> fetchFundingSources(JsonNode indexDocument) {
        return extractUris(fundingNodes(indexDocument), SOURCE).stream()
                   .map(uri -> extractResponseBody(uri, fetch(uri)))
                   .toList();
    }

    private String extractResponseBody(URI uri, HttpResponse<String> response) {
        if (response.statusCode() / ONE_HUNDRED == SUCCESS_FAMILY) {
            return response.body();
        } else if (isClientError(response)) {
            logger.warn("Client error when fetching funding source: {}. Response body: {}", uri, response.body());
            return FundingSource.withId(uri).toJsonString();
        } else {
            throw new RuntimeException("Unexpected response " + response);
        }
    }

    private boolean isClientError(HttpResponse<String> response) {
        return response.statusCode() / ONE_HUNDRED == CLIENT_ERROR_FAMILY;
    }

    @Deprecated
    private InputStream addPotentiallyMissingContext(InputStream inputStream) {
        return attempt(() -> objectMapper.readTree(inputStream))
                   .toOptional()
                   .filter(this::hasNoContextNode)
                   .map(this::injectContext)
                   .map(JsonNode::toString)
                   .map(IoUtils::stringToStream)
                   .orElseGet(() -> resetInputStream(inputStream));
    }

    private InputStream resetInputStream(InputStream inputStream) {
        try {
            inputStream.reset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return inputStream;
    }

    private JsonNode injectContext(JsonNode jsonNode) {
        return ((ObjectNode) jsonNode).set(CONTEXT, CONTEXT_NODE);
    }

    private boolean hasNoContextNode(JsonNode jsonNode) {
        return !jsonNode.has(CONTEXT);
    }

    private Collection<? extends InputStream> fetchAll(Collection<URI> uris, JsonNode indexDocument) {
        return uris.stream()
                   .filter(Objects::nonNull)
                   .map(this::fetch)
                   .map(response -> processResponse(response, indexDocument))
                   .toList();
    }

    private InputStream processResponse(HttpResponse<String> response, JsonNode indexDocument) {
        if (response.statusCode() / ONE_HUNDRED == SUCCESS_FAMILY) {
            var body = response.body();
            return stringToStream(removeTypeToIgnoreWhatTheWorldDefinesThisResourceAs(body));
        } else {
            var identifier = new SortableIdentifier(indexDocument.get("identifier").asText());
            RecoveryEntry.create(RecoveryEntry.RESOURCE, identifier)
                .withException(new Exception(response.toString()))
                .persist(queueClient);
            return null;
        }
    }

    private String removeTypeToIgnoreWhatTheWorldDefinesThisResourceAs(String body) {
        var objectNode = (ObjectNode) attempt(() -> objectMapper.readTree(body)).orElseThrow();
        objectNode.remove(TYPE);
        return attempt(() -> objectMapper.writeValueAsString(objectNode)).orElseThrow();
    }

    private Optional<InputStream> fetchAnthologyContent(JsonNode indexDocument) {
        return isAcademicChapter(indexDocument) || isPublicationContextTypeAnthology(indexDocument)
                   ? getAnthology(indexDocument)
                   : Optional.empty();
    }

    private Optional<InputStream> getAnthology(JsonNode indexDocument) {
        return extractPublicationContextUri(indexDocument)
                   .map(uri -> new ExpandedParentPublication(uriRetriever, resourceService, queueClient)
                                   .getExpandedParentPublication(uri))
                   .map(IoUtils::stringToStream);
    }

    private HttpResponse<String> fetch(URI externalReference) {
        return uriRetriever.fetchResponse(externalReference, APPLICATION_JSON_LD.toString()).orElseThrow();
    }
}
