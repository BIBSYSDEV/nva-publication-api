package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.ioutils.IoUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

public class ExpandedParentPublication {

    private static final String EMPTY_STRING = "";
    private static final String PUBLICATION_ONTOLOGY = "https://nva.sikt.no/ontology/publication#Publication";
    private final UriRetriever uriRetriever;

    public ExpandedParentPublication(UriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String getExpandedParentPublication(URI publicationId) throws JsonProcessingException {
        var publicationResponseBody = fetch(publicationId);
        if (publicationResponseBody.isEmpty()) {
            return EMPTY_STRING;
        }
        var model = createDefaultModel();
        loadPublicationWithChannelDataIntoModel(publicationResponseBody.get(), model);
        removePublicationTypeFromResource(publicationId, model);
        return writeModelAsJsonLdString(model);
    }

    private static String writeModelAsJsonLdString(Model model) {
        var stringWriter = new StringWriter();
        RDFDataMgr.write(stringWriter, model, Lang.JSONLD);
        return stringWriter.toString();
    }

    private static void removePublicationTypeFromResource(URI id, Model model) {
        var publicationType = model.createResource(PUBLICATION_ONTOLOGY);
        model.remove(model.createStatement(model.createResource(id.toString()), RDF.type, publicationType));
    }

    private void loadPublicationWithChannelDataIntoModel(String publicationJsonString, Model model) throws JsonProcessingException {
        var inputStreams = getInputStreams(publicationJsonString);
        inputStreams.forEach(inputStream -> RDFDataMgr.read(model, inputStream, Lang.JSONLD));
    }

    private List<InputStream> getInputStreams(String publicationJsonString)
        throws JsonProcessingException {
        var inputStreams = new ArrayList<InputStream>();
        inputStreams.add(stringToStream(publicationJsonString));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(objectMapper.readTree(publicationJsonString))));
        return inputStreams;
    }

    private Collection<? extends InputStream> fetchAll(List<URI> externalReferences) {
        return externalReferences.stream()
                   .map(this::fetch)
                   .flatMap(Optional::stream)
                   .map(IoUtils::stringToStream)
                   .collect(Collectors.toList());
    }

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }
}
