package no.unit.nva.publication.utils;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.JacocoGenerated;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class RdfUtils {
    private static final String PART_OF_PROPERTY = "https://nva.sikt.no/ontology/publication#partOf";
    public static final String APPLICATION_JSON = "application/json";
    public static final Logger logger = LoggerFactory.getLogger(RdfUtils.class);

    public static List<URI> getAllNestedPartOfs(UriRetriever uriRetriever, URI organizationId) {
        return attempt(() -> uriRetriever.getRawContent(organizationId, APPLICATION_JSON)).map(
                Optional::orElseThrow)
                   .map(str -> createModel(stringToStream(str)))
                   .map(model -> model.listObjectsOfProperty(model.createProperty(PART_OF_PROPERTY)))
                   .map(nodeIterator -> nodeIterator.toList()
                                            .stream().map(RDFNode::toString).map(URI::create).toList())
                   .orElseThrow();

    }
    public static URI getTopLevelOrgUri(UriRetriever uriRetriever, URI id) {
        var data = attempt(() -> uriRetriever.getRawContent(id, APPLICATION_JSON)).orElseThrow();

        if (data.isEmpty()) {
            return id;
        }

        var model = createModel(stringToStream(data.get()));
        var query = getTopLevelQuery();

        return Optional.ofNullable(getFirstResultFromQuery(query, model)).orElse(logAndReturnDefaultId(id, data.get()));
    }

    private static URI logAndReturnDefaultId(URI id, String data) {
        logger.warn("Could not find topLevel of org {}", id);
        logger.warn(data);
        return id;
    }

    private static URI getFirstResultFromQuery(Query query, Model model) {
        try (var qe = QueryExecutionFactory.create(query, model)) {
            var result = qe.execSelect();
            if (result.hasNext()) {
                return URI.create(result.next().get("organization").asResource().getURI());
            }
        }
        return null;
    }

    private static Query getTopLevelQuery() {
        return QueryFactory.create("prefix : <https://nva.sikt.no/ontology/publication#> "
                                   + "SELECT ?organization WHERE {"
                                   + "?organization a :Organization ."
                                   + "OPTIONAL {?somethingelse :hasPart ?organization}"
                                   + "OPTIONAL {?organization :partOf ?somethingelse}"
                                   + "FILTER (!BOUND(?somethingelse))"
                                   + "}");
    }

    public static Model createModel(InputStream inputStream) {
        var model = ModelFactory.createDefaultModel();

        if (isNull(inputStream)) {
            return model;
        }
        try {
            RDFDataMgr.read(model, inputStream, Lang.JSONLD);
        } catch (RiotException e) {
            logger.warn("Invalid JSON LD input encountered: ", e);
        }
        return model;
    }
}
