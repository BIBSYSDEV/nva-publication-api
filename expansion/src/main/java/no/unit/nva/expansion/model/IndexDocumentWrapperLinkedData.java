package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.model.ExpandedResource.getPublicationContextUris;
import static no.unit.nva.expansion.utils.JsonLdUtils.toJsonString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
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
    
    private final UriRetriever uriRetriever;
    
    public IndexDocumentWrapperLinkedData(UriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }
    
    public String toFramedJsonLd(JsonNode indexDocument) throws IOException {
        String frame = SearchIndexFrame.FRAME_SRC;
        List<InputStream> inputStreams = getInputStreams(indexDocument);
        return new FramedJsonGenerator(inputStreams, stringToStream(frame)).getFramedJson();
    }
    
    private List<InputStream> getInputStreams(JsonNode indexDocument) {
        final List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(stringToStream(toJsonString(indexDocument)));
        inputStreams.addAll(fetchAll(getPublicationContextUris(indexDocument)));
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
    
    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }
}
