package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static no.unit.nva.expansion.utils.JsonLdDefaults.frameJsonLd;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.StringUtils;
import nva.commons.core.ioutils.IoUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

public class ExpandedParentPublication {

    public static final JsonDocument FRAME;

    static {
        try {
            FRAME = JsonDocument.of(IoUtils.inputStreamFromResources("parentPublicationFrame.json"));
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    private static final String PUBLICATION_ONTOLOGY = "https://nva.sikt.no/ontology/publication#Publication";
    private final RawContentRetriever uriRetriever;

    public ExpandedParentPublication(RawContentRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String getExpandedParentPublication(URI publicationId) {
        return expandParent(publicationId, fetch(publicationId));
    }

    private static void removePublicationTypeFromResource(URI id, Model model) {
        var publicationType = model.createResource(PUBLICATION_ONTOLOGY);
        model.remove(model.createStatement(model.createResource(id.toString()), RDF.type, publicationType));
    }

    private String expandParent(URI publicationId, String publicationResponseBody) {
        var model = createDefaultModel();
        loadPublicationWithChannelDataIntoModel(publicationResponseBody, model);
        removePublicationTypeFromResource(publicationId, model);
        return frameJsonLd(model, FRAME);
    }

    private void loadPublicationWithChannelDataIntoModel(String publicationJsonString, Model model) {
        var inputStreams = getInputStreams(publicationJsonString);
        inputStreams.forEach(inputStream -> RDFDataMgr.read(model, inputStream, Lang.JSONLD));
    }

    private List<InputStream> getInputStreams(String publicationJsonString) {
        var inputStreams = new ArrayList<InputStream>();
        inputStreams.add(stringToStream(publicationJsonString));
        inputStreams.addAll(fetchAll(
            extractPublicationContextUris(attempt(() -> objectMapper.readTree(publicationJsonString)).orElseThrow())));
        return inputStreams;
    }

    private Collection<? extends InputStream> fetchAll(List<URI> externalReferences) {
        return externalReferences.stream()
                   .filter(this::isNotBlankUri)
                   .map(this::fetch)
                   .map(IoUtils::stringToStream)
                   .toList();
    }

    private boolean isNotBlankUri(URI uri) {
        return StringUtils.isNotBlank(uri.toString());
    }

    private String fetch(URI externalReference) {
        return uriRetriever.fetchResponse(externalReference, APPLICATION_JSON_LD.toString())
                   .filter(response -> response.statusCode() / 100 == 2)
                   .map(HttpResponse::body)
                   .orElseThrow();
    }
}
