package no.unit.nva.expansion.model;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
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
import java.util.HashMap;
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
import nva.commons.core.Environment;
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
        inputStreams.addAll(fetchFundingSources(indexDocument));
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
                       .filter(response -> response.statusCode() == 200)
                       .map(HttpResponse::body)
                       .map(this::toNviCandidateResponse)
                       .map(NviCandidateResponse::toNviStatus)
                       .filter(ScientificIndex::isReported)
                       .map(ScientificIndex::toJsonNode)
                       .orElse(null);
        } catch (Exception e) {
            logger.error(EXCEPTION, e.toString());
            throw ExpansionException.withMessage(String.format(FETCHING_NVI_CANDIDATE_ERROR_MESSAGE, publicationId));
        }


    }

    private Optional<HttpResponse<String>> fetchNviCandidate(String publicationId) {
        return attempt(() -> uriRetriever.fetchResponse(fetchNviCandidateUri(publicationId), "application/json"))
                   .orElseThrow();
    }

    private NviCandidateResponse toNviCandidateResponse(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, NviCandidateResponse.class)).orElseThrow();
    }

    private static URI fetchNviCandidateUri(String publicationId) {
        var uri = UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("scientific-index")
                   .addChild("candidate")
                   .addChild("publication")
                   .getUri();
        return URI.create(uri + "/" + publicationId);
    }

    @Deprecated
    private Collection<? extends InputStream> fetchFundingSources(JsonNode indexDocument) {
        return fetchFundings(indexDocument).stream()
                   .map(IoUtils::stringToStream)
                   .map(this::addPotentiallyMissingContext)
                   .collect(toList());
    }

    private Collection<String> fetchFundings(JsonNode indexDocument) {
        var fundingIdentifiers = extractUris(fundingNodes(indexDocument), SOURCE);
        var fundingMap = new HashMap<URI, String>();
        for (URI uri : fundingIdentifiers) {
            fundingMap.put(uri, this.fetchUri(uri));
        }
        fundingMap.replaceAll(IndexDocumentWrapperLinkedData::replaceNotFetchedFundingSource);
        return fundingMap.values();
    }

    private static String replaceNotFetchedFundingSource(URI key, String value) {
        return nonNull(value)
                   ? value
                   : FundingSource.withId(key).toJsonString();
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
                   .map(this::fetch)
                   .flatMap(Optional::stream)
                   .map(IoUtils::stringToStream)
                   .collect(toList());
    }

    private Collection<? extends InputStream> fetchAllAffiliationContent(JsonNode indexDocument) {
        return extractAffiliationUris(indexDocument)
                   .stream()
                   .distinct()
                   .map(this::fetchOrganization)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(CristinOrganization::toJsonString)
                   .map(IoUtils::stringToStream)
                   .collect(toList());
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

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }

    private String fetchUri(URI externalReference) {
        var response = uriRetriever.fetchResponse(externalReference, APPLICATION_JSON_LD.toString());
        return Optional.ofNullable(response)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .filter(httpResponse -> httpResponse.statusCode() == HTTP_OK)
                   .map(HttpResponse::body)
                   .orElse(null);
    }

    private Optional<CristinOrganization> fetchOrganization(URI externalReference) {
        var rawContent = uriRetriever.getRawContent(externalReference, MEDIA_TYPE_JSON_LD_V2);
        return rawContent.isPresent()
                   ? attempt(() -> objectMapper.readValue(rawContent.get(), CristinOrganization.class)).toOptional()
                   : Optional.empty();
    }
}
