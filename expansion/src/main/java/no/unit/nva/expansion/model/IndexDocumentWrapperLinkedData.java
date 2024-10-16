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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.expansion.model.nvi.NviCandidateResponse;
import no.unit.nva.expansion.model.nvi.ScientificIndex;
import no.unit.nva.expansion.utils.FramedJsonGenerator;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexDocumentWrapperLinkedData {

    private static final Logger logger = LoggerFactory.getLogger(IndexDocumentWrapperLinkedData.class);
    public static final String CRISTIN_VERSION = "; version=2023-05-26";
    private static final String MEDIA_TYPE_JSON_LD_V2 = APPLICATION_JSON_LD.toString() + CRISTIN_VERSION;
    private static final String SOURCE = "source";
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
    public static final String FETCHING_NVI_CANDIDATE_ERROR_MESSAGE =
        "Could not fetch nvi candidate for publication with identifier: %s";
    public static final String EXCEPTION = "Exception {}:";
    public static final String ID = "id";
    public static final String SCIENTIFIC_INDEX = "scientific-index";
    public static final String CANDIDATE = "candidate";
    public static final String PUBLICATION = "publication";
    public static final String PATH_DELIMITER = "/";
    private final RawContentRetriever uriRetriever;

    public IndexDocumentWrapperLinkedData(RawContentRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String toFramedJsonLd(JsonNode indexDocument) {
        var frame = SearchIndexFrame.FRAME_SRC;
        var inputStreams = getInputStreams(indexDocument);
        return new FramedJsonGenerator(inputStreams, frame).getFramedJson();
    }

    //TODO: parallelize

    private List<InputStream> getInputStreams(JsonNode indexDocument) {
        final List<InputStream> inputStreams = new ArrayList<>();
        injectScientificIndexStatus(indexDocument);
        inputStreams.add(stringToStream(toJsonString(indexDocument)));
        fetchAnthologyContent(indexDocument).ifPresent(inputStreams::add);
        inputStreams.addAll(fetchAllAffiliationContent(indexDocument));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(indexDocument)));
        inputStreams.addAll(fetchFundingSourcesAddingContext(indexDocument));
        inputStreams.removeIf(Objects::isNull);
        return inputStreams;
    }

    private void injectScientificIndexStatus(JsonNode indexDocument) {
        ((ObjectNode) indexDocument).set(ScientificIndex.SCIENTIFIC_INDEX_FIELD, fetchNviStatus(indexDocument));
    }

    private JsonNode fetchNviStatus(JsonNode indexDocument) {
        var publicationId = indexDocument.get(ID).asText();
        var urlEncodedPublicationId = URLEncoder.encode(publicationId, StandardCharsets.UTF_8);
        try {
            return fetchNviCandidate(urlEncodedPublicationId)
                       .filter(IndexDocumentWrapperLinkedData::isAcceptableNviResponse)
                       .map(this::processNviCandidateResponse)
                       .orElseThrow();
        } catch (Exception e) {
            logger.error(EXCEPTION, e.toString());
            throw ExpansionException.withMessage(String.format(FETCHING_NVI_CANDIDATE_ERROR_MESSAGE, publicationId));
        }
    }

    private JsonNode processNviCandidateResponse(HttpResponse<String> response) {
        if (response.statusCode() == 404) {
            return new ObjectNode(null);
        } else {
            var nviStatus = toNviCandidateResponse(response.body()).toNviStatus();
            return nviStatus.isReported() ? nviStatus.toJsonNode() : new ObjectNode(null);
        }
    }

    private static boolean isAcceptableNviResponse(HttpResponse<String> response) {
        return response.statusCode() / 100 == 2 || response.statusCode() == 404;
    }

    private Optional<HttpResponse<String>> fetchNviCandidate(String publicationId) {
        return attempt(() -> uriRetriever.fetchResponse(fetchNviCandidateUri(publicationId), "application/json"))
                   .orElseThrow();
    }

    private NviCandidateResponse toNviCandidateResponse(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, NviCandidateResponse.class)).orElseThrow();
    }

    private static URI fetchNviCandidateUri(String publicationId) {
        var uri = UriWrapper.fromHost(API_HOST)
                   .addChild(SCIENTIFIC_INDEX)
                   .addChild(CANDIDATE)
                   .addChild(PUBLICATION)
                   .getUri();
        return URI.create(String.format("%s/%s", uri, publicationId));
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
                   .map(uri -> {
                       var response = fetch(uri);
                       if (response.statusCode() / 100 == 2) {
                           return response.body();
                       } else if (isClientError(response)) {
                           logger.warn("Client error when fetching funding source: {}. Response body: {}", uri,
                                       response.body());
                           return FundingSource.withId(uri).toJsonString();
                       } else {
                           throw new RuntimeException("Unexpected response " + response);
                       }
                   })
                   .toList();
    }

    private boolean isClientError(HttpResponse<String> response) {
        return response.statusCode() / 100 == 4;
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

    private Collection<? extends InputStream> fetchAll(Collection<URI> uris) {
        return uris.stream()
                   .filter(Objects::nonNull)
                   .map(this::fetch)
                   .map(this::processResponse)
                   .toList();
    }

    private InputStream processResponse(HttpResponse<String> response) {
        if (response.statusCode() / 100 == 2) {
            return IoUtils.stringToStream(response.body());
        }
        throw new RuntimeException("Unexpected response " + response);
    }

    private Collection<? extends InputStream> fetchAllAffiliationContent(JsonNode indexDocument) {
        return extractAffiliationUris(indexDocument)
                   .stream()
                   .distinct()
                   .map(this::fetchOrganization)
                   .map(CristinOrganization::toJsonString)
                   .map(IoUtils::stringToStream)
                   .toList();
    }

    private Optional<InputStream> fetchAnthologyContent(JsonNode indexDocument) {
        return isAcademicChapter(indexDocument) || isPublicationContextTypeAnthology(indexDocument)
                   ? getAnthology(indexDocument)
                   : Optional.empty();
    }

    private Optional<InputStream> getAnthology(JsonNode indexDocument) {
        return extractPublicationContextUri(indexDocument)
                   .map(uri -> new ExpandedParentPublication(uriRetriever)
                                   .getExpandedParentPublication(uri))
                   .map(IoUtils::stringToStream);
    }

    private HttpResponse<String> fetch(URI externalReference) {
        return uriRetriever.fetchResponse(externalReference, APPLICATION_JSON_LD.toString()).orElseThrow();
    }

    private CristinOrganization fetchOrganization(URI externalReference) {
        var rawContent = uriRetriever.getRawContent(externalReference, MEDIA_TYPE_JSON_LD_V2).orElseThrow();
        return attempt(() -> objectMapper.readValue(rawContent, CristinOrganization.class)).orElseThrow();
    }
}
