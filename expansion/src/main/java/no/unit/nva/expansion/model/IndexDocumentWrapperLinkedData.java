package no.unit.nva.expansion.model;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.contributorNodes;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextId;
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
import java.io.InputStream;
import java.net.URI;
import java.util.*;
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
    private static final String EMPTY_STRING = "";
    private final UriRetriever uriRetriever;

    public IndexDocumentWrapperLinkedData(UriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String toFramedJsonLd(JsonNode indexDocument) {
        String frame = SearchIndexFrame.FRAME_SRC;
        List<InputStream> inputStreams = getInputStreams(indexDocument);
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
        inputStreams.add(fetchAnthologyContent(indexDocument));
        inputStreams.addAll(fetchAllAffiliationContent(indexDocument));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(indexDocument)));
        inputStreams.addAll(fetchAll(extractUris(fundingNodes(indexDocument), "source")));
        inputStreams.removeIf(Objects::isNull);
        return inputStreams;
    }

    private Collection<? extends InputStream> fetchAll(Collection<URI> uris) {
        return uris.stream()
                   .distinct()
                   .map(this::fetch)
                   .flatMap(Optional::stream)
                   .map(IoUtils::stringToStream)
                   .collect(Collectors.toList());
    }

    private Collection<? extends InputStream> fetchAllAffiliationContent(JsonNode indexDocument) {
        return extractUris(contributorNodes(indexDocument), "/id")
                   .stream()
                   .distinct()
                   .flatMap(this::fetchContentRecursively)
                   .map(IoUtils::stringToStream)
                   .collect(Collectors.toList());
    }

    private InputStream fetchAnthologyContent(JsonNode indexDocument) {
        return isAcademicChapter(indexDocument) && isPublicationContextTypeAnthology(indexDocument)
                   ? stringToStream(getAnthology(indexDocument))
                   : null;
    }

    private String getAnthology(JsonNode indexDocument) {
        var anthologyUri = extractPublicationContextId(indexDocument);
        return new ExpandedParentPublication(uriRetriever).getExpandedParentPublication(anthologyUri);
    }

    private Stream<String> fetchContentRecursively(URI uri) {
        var optionalDocument = fetch(uri);
        if (optionalDocument.isEmpty()) {
            return Stream.empty();
        }
        var parentAffiliations = toStreamJsonNode(optionalDocument.get())
                                     .map(IndexDocumentWrapperLinkedData::extractIdField)
                                     .map(URI::create)
                                     .flatMap(this::fetchContentRecursively);
        return Stream.concat(Stream.of(optionalDocument.get()), parentAffiliations);
    }

    private static Stream<JsonNode> toStreamJsonNode(String document) {
        var jsonNode = attempt(() -> dtoObjectMapper.readTree(document)).orElseThrow().at(PART_OF_FIELD);
        return toStream(jsonNode);
    }

    private Optional<String> fetch(URI externalReference) {

        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }
}
