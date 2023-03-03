package no.unit.nva.expansion.model;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.extractAffiliationUris;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static no.unit.nva.expansion.utils.JsonLdUtils.toJsonString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.expansion.utils.FramedJsonGenerator;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.ioutils.IoUtils;

public class IndexDocumentWrapperLinkedData {

    public static final String PART_OF_FIELD = "/partOf";
    public static final String ID_FIELD = "/id";
    private final UriRetriever uriRetriever;

    public IndexDocumentWrapperLinkedData(UriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String toFramedJsonLd(JsonNode indexDocument) {
        String frame = SearchIndexFrame.FRAME_SRC;
        List<InputStream> inputStreams = getInputStreams(indexDocument);
        return new FramedJsonGenerator(inputStreams, frame).getFramedJson();
    }

    //TODO: parallelize
    private List<InputStream> getInputStreams(JsonNode indexDocument) {
        final List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(stringToStream(toJsonString(indexDocument)));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(indexDocument)));
        inputStreams.addAll(fetchAllAffiliationContent(indexDocument));
        return inputStreams;
    }

    private Collection<? extends InputStream> fetchAll(List<URI> publicationContextUris) {
        return publicationContextUris.stream()
                   .map(this::fetch)
                   .flatMap(Optional::stream)
                   .map(IoUtils::stringToStream)
                   .collect(Collectors.toList());
    }

    private List<InputStream> fetchAllAffiliationContent(JsonNode indexDocument) {
        return extractAffiliationUris(indexDocument)
                   .stream().flatMap(this::fetchContentRecursively)
                   .map(IoUtils::stringToStream)
                   .collect(Collectors.toList());
    }

    private Stream<String> fetchContentRecursively(URI uri) {
        var affiliation = fetch(uri);
        if (affiliation.isEmpty()) {
            return Stream.empty();
        }
        var parentAffiliations = extractParentAffiliationNodes(affiliation.get())
                                     .map(IndexDocumentWrapperLinkedData::extractIdField)
                                     .map(URI::create)
                                     .flatMap(this::fetchContentRecursively);
        return Stream.concat(Stream.of(affiliation.get()), parentAffiliations);
    }

    private static String extractIdField(JsonNode i) {
        return i.at(ID_FIELD).asText();
    }

    private static Stream<JsonNode> extractParentAffiliationNodes(String affiliation) {
        var json = attempt(() -> dtoObjectMapper.readTree(affiliation)).orElseThrow().at(PART_OF_FIELD);
        return toStream(json);
    }

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }

    private static Stream<JsonNode> toStream(JsonNode jsonNode) {
        return StreamSupport.stream(jsonNode.spliterator(), false);
    }
}
