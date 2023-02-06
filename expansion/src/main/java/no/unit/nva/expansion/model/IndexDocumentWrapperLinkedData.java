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
import no.unit.nva.expansion.utils.FramedJsonGenerator;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.expansion.utils.UriRetriever;
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
        List<Optional<String>> uriContent =
            publicationContextUris.stream().map(this::fetch).collect(Collectors.toList());
        return uriContent.stream()
            .flatMap(Optional::stream)
            .map(IoUtils::stringToStream)
            .collect(Collectors.toList());
    }

    private Collection<? extends InputStream> fetchAllAffiliationContent(JsonNode indexDocument) {
        List<Optional<String>> uriContent = new ArrayList<>();

        var affiliationUris = extractAffiliationUris(indexDocument);

        affiliationUris.forEach(uri -> fetchContentRecursively(uriContent, uri));

        return uriContent.stream()
            .flatMap(Optional::stream)
            .map(IoUtils::stringToStream)
            .collect(Collectors.toList());
    }

    private void fetchContentRecursively(List<Optional<String>> contentList, URI uri) {
        var affiliation = fetch(uri);
        contentList.add(affiliation);

        if (affiliation.isEmpty()) {
            return;
        }

        var json = attempt(() -> dtoObjectMapper.readTree(affiliation.get())).orElseThrow();

        var parentAffiliations = json.at(PART_OF_FIELD);
        parentAffiliations.forEach(
            parentAff -> fetchContentRecursively(contentList, URI.create(parentAff.at(ID_FIELD).asText()))
        );
    }

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }
}
