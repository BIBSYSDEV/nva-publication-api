package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.ResourceExpansionServiceImpl.API_HOST;
import static no.unit.nva.expansion.model.ExpandedResource.extractAffiliationUris;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUri;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static no.unit.nva.expansion.model.ExpandedResource.isAcademicChapter;
import static no.unit.nva.expansion.model.ExpandedResource.isPublicationContextTypeAnthology;
import static no.unit.nva.expansion.utils.JsonLdUtils.toJsonString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import no.unit.nva.expansion.model.cristin.CristinOrganization;
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
    private static final String FRAME_JSON = "frame.json";
    private static final String FETCHING_NVI_CANDIDATE_ERROR_MESSAGE =
        "Could not fetch nvi candidate for publication with identifier: %s";
    private static final String EXCEPTION = "Exception {}:";
    private static final String ID = "id";
    private static final String SCIENTIFIC_INDEX = "scientific-index";
    private static final String PUBLICATION = "publication";
    private static final int ONE_HUNDRED = 100;
    private static final int SUCCESS_FAMILY = 2;
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
        return new FramedJsonGenerator(inputStreams, frame, uriRetriever).getFramedJson();
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
        inputStreams.addAll(fetchAllAffiliationContent(indexDocument));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(indexDocument), indexDocument));
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

    private Collection<? extends InputStream> fetchAll(
            Collection<URI> uris, JsonNode indexDocument) {
        var documentIdentifier = new SortableIdentifier(indexDocument.get("identifier").asText());
        return uris.stream()
                .filter(Objects::nonNull)
                .map(this::fetch)
                .map(response -> processResponse(response, documentIdentifier))
                .filter(Objects::nonNull)
                .toList();
    }

    private InputStream processResponse(
            HttpResponse<String> response, SortableIdentifier documentIdentifier) {
        if (response.statusCode() / ONE_HUNDRED == SUCCESS_FAMILY) {
            var body = response.body();
            return stringToStream(removeTypeToIgnoreWhatTheWorldDefinesThisResourceAs(body));
        } else {
            createRecoveryMessage(response, documentIdentifier);
            return null;
        }
    }

    private void createRecoveryMessage(
            HttpResponse<String> response, SortableIdentifier identifier) {
        RecoveryEntry.create(RecoveryEntry.RESOURCE, identifier)
                .withException(new Exception(response.toString()))
                .persist(queueClient);
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

    private Optional<CristinOrganization> fetchOrganization(
            URI externalReference, SortableIdentifier documentIdentifier) {
        var response = fetch(externalReference);
        if (response.statusCode() / ONE_HUNDRED == SUCCESS_FAMILY) {
            var body = response.body();
            return Optional.of(mapToCristinOrganization(body));
        } else {
            createRecoveryMessage(response, documentIdentifier);
            return Optional.empty();
        }
    }

    private Collection<? extends InputStream> fetchAllAffiliationContent(JsonNode indexDocument) {
        var documentIdentifier = new SortableIdentifier(indexDocument.get("identifier").asText());
        return extractAffiliationUris(indexDocument).stream()
                .distinct()
                .map(organizationId -> fetchOrganization(organizationId, documentIdentifier))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CristinOrganization::toJsonString)
                .map(IoUtils::stringToStream)
                .toList();
    }

    private CristinOrganization mapToCristinOrganization(String response) {
        return attempt(() -> objectMapper.readValue(response, CristinOrganization.class))
                .orElseThrow();
    }
}
