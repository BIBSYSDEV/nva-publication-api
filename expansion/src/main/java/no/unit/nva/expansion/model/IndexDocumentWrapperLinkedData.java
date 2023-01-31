package no.unit.nva.expansion.model;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.utils.FramedJsonGenerator;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.expansion.utils.UriRetriever;
import nva.commons.core.ioutils.IoUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.unit.nva.expansion.model.ExpandedResource.extractAffiliationUris;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static no.unit.nva.expansion.utils.JsonLdUtils.toJsonString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;

public class IndexDocumentWrapperLinkedData {

    public static final String PART_OF_FIELD = "/partOf";
    public static final String ID_FIELD = "/id";
    private final UriRetriever uriRetriever;

    private static final String ORGANIZATION_TYPE = "Organization";
    private static final String TYPE_FIELD = "type";


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
        inputStreams.addAll(fetchAll(extractAffiliationUris(indexDocument)));
        return inputStreams;
    }

    private Collection<? extends InputStream> fetchAll(List<URI> publicationContextUris) {
        List<Optional<String>> uriContent = new ArrayList<>();

        for (URI uri : publicationContextUris) {
            var content = fetch(uri);
            uriContent.add(content);

            content.ifPresent(cont -> fetchChildUrisForOrganizations(uriContent, cont));
        }

        return uriContent.stream()
                .flatMap(Optional::stream)
                .map(IoUtils::stringToStream)
                .collect(Collectors.toList());
    }

    private void fetchChildUrisForOrganizations(List<Optional<String>> uriContent, String content) {
        JsonNode json = attempt(() -> JsonUtils.dtoObjectMapper.readTree(content)).orElseThrow();

        if (json.has(TYPE_FIELD) && ORGANIZATION_TYPE.equals(json.at("/" + TYPE_FIELD).asText())) {
            var childOrgs = json.at(PART_OF_FIELD);
            childOrgs.forEach(org ->
                    uriContent.add(fetch(URI.create(org.at(ID_FIELD).asText())))
            );
        }
    }

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }
}
