package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static no.unit.nva.expansion.utils.JsonLdDefaults.frameJsonLd;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import com.apicatalog.jsonld.document.JsonDocument;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.StringUtils;
import nva.commons.core.ioutils.IoUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

public class ExpandedParentPublication {

    private static final JsonDocument FRAME = SearchIndexFrame
                                                  .getFrameWithContext(Path.of("parentPublicationFrame.json"));
    private static final String PARENT_PUBLICATION_NOT_FOUND_S = "Parent publication not found %s";

    private static final String PUBLICATION_ONTOLOGY = "https://nva.sikt.no/ontology/publication#Publication";
    private static final String ERROR_MESSAGE_FETCHING_REFERENCE = "Could not fetch external reference: %s";
    private static final int ONE_HUNDRED = 100;
    private static final int SUCCESS_FAMILY = 2;
    private static final int CLIENT_ERROR_FAMILY = 4;
    private final RawContentRetriever uriRetriever;
    private final ResourceService resourceService;

    public ExpandedParentPublication(RawContentRetriever uriRetriever, ResourceService resourceService) {
        this.uriRetriever = uriRetriever;
        this.resourceService = resourceService;
    }

    public String getExpandedParentPublication(URI publicationId) {
        return expandParent(publicationId, fetchParentPublication(publicationId));
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

    private boolean processResponse(HttpResponse<String> response) {
        if (response.statusCode() / ONE_HUNDRED == SUCCESS_FAMILY) {
            return true;
        } else if (response.statusCode() / ONE_HUNDRED == CLIENT_ERROR_FAMILY) {
            return true;
        }
        throw new RuntimeException("Unexpected response " + response);
    }

    private String fetch(URI externalReference) {
        return uriRetriever.fetchResponse(externalReference, APPLICATION_JSON_LD.toString())
                   .filter(this::processResponse)
                   .map(HttpResponse::body)
                   .orElseThrow(() -> new RuntimeException(
                       ERROR_MESSAGE_FETCHING_REFERENCE.formatted(externalReference)));
    }

    private String fetchParentPublication(URI publicationId) {
        var publicationIdentifier = SortableIdentifier.fromUri(publicationId);
        var publication = fetchPublication(publicationIdentifier);
        return PublicationMapper.convertValue(publication, PublicationResponseElevatedUser.class).toJsonString();
    }

    private Publication fetchPublication(SortableIdentifier publicationIdentifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(publicationIdentifier))
                   .orElseThrow(failure -> new RuntimeException(
                       PARENT_PUBLICATION_NOT_FOUND_S.formatted(publicationIdentifier)));
    }
}
