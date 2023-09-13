package no.unit.nva.expansion.model;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.expansion.utils.FramedJsonGenerator;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.ioutils.IoUtils;

public class IndexDocumentWrapperLinkedData {

    private static final String PART_OF_FIELD = "/partOf";
    private static final String ID_FIELD = "/id";
    private static final String SOURCE = "source";
    private static final String CONTEXT = "@context";
    public static final String CRISTIN_VERSION = "; version=2023-05-26";
    private final UriRetriever uriRetriever;

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

    public IndexDocumentWrapperLinkedData(UriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String toFramedJsonLd(JsonNode indexDocument) {
        var frame = SearchIndexFrame.FRAME_SRC;
        var inputStreams = getInputStreams(indexDocument);
        return new FramedJsonGenerator(inputStreams, frame).getFramedJson();
    }

    private static String extractIdField(JsonNode i) {
        return i.at(ID_FIELD).asText();
    }

    private static Stream<JsonNode> extractParentAffiliationNodes(String affiliation) {
        return attempt(() -> dtoObjectMapper.readTree(affiliation))
                   .map(IndexDocumentWrapperLinkedData::extractPartOfNode)
                   .map(IndexDocumentWrapperLinkedData::toStream)
                   .orElseThrow();
    }

    private static JsonNode extractPartOfNode(JsonNode node) {
        return node.at(PART_OF_FIELD);
    }

    private static Stream<JsonNode> toStream(JsonNode jsonNode) {
        return StreamSupport.stream(jsonNode.spliterator(), false);
    }

    //TODO: parallelize
    private List<InputStream> getInputStreams(JsonNode indexDocument) {
        final List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(stringToStream(toJsonString(indexDocument)));
        fetchAnthologyContent(indexDocument).ifPresent(inputStreams::add);
        inputStreams.addAll(fetchAllAffiliationContent(indexDocument));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(indexDocument)));
        inputStreams.addAll(fetchFundingSources(indexDocument));
        inputStreams.removeIf(Objects::isNull);
        return inputStreams;
    }


    @Deprecated
    private Collection<? extends InputStream> fetchFundingSources(JsonNode indexDocument) {
        return fetchAll(extractUris(fundingNodes(indexDocument), SOURCE))
                   .stream()
                   .map(this::addPotentiallyMissingContext)
                   .collect(Collectors.toList());
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
                   .collect(Collectors.toList());
    }

    private Collection<? extends InputStream> fetchAllAffiliationContent(JsonNode indexDocument) {
        return extractAffiliationUris(indexDocument)
                   .stream()
                   .distinct()
                   .flatMap(this::fetchContentRecursively)
                   .map(IoUtils::stringToStream)
                   .collect(Collectors.toList());
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


    private Stream<String> fetchContentRecursively(URI uri) {
        var affiliation = fetchOrganizations(uri);
        if (affiliation.isEmpty()) {
            return Stream.empty();
        }
        var parentAffiliations = extractParentAffiliationNodes(affiliation.get())
                                     .map(IndexDocumentWrapperLinkedData::extractIdField)
                                     .map(URI::create)
                                     .flatMap(this::fetchContentRecursively);
        return Stream.concat(Stream.of(affiliation.get()), parentAffiliations);
    }

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }

    private Optional<String> fetchOrganizations(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString() + CRISTIN_VERSION);
    }
}
