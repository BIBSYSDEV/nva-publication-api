package no.unit.nva.expansion.utils;

import static java.util.Objects.isNull;
import static no.unit.nva.expansion.utils.JsonLdDefaults.frameJsonLd;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static org.apache.http.HttpStatus.SC_OK;
import com.apicatalog.jsonld.document.Document;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.expansion.model.FundingSource;
import nva.commons.core.JacocoGenerated;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class FramedJsonGenerator {

    private static final Logger logger = LoggerFactory.getLogger(FramedJsonGenerator.class);
    private static final String PROJECT_PROPERTY_URI = "https://nva.sikt.no/ontology/publication#project";
    private static final String PUBLICATION_CLASS_URI = "https://nva.sikt.no/ontology/publication#Publication";
    private static final String SOURCE_PROPERTY_URI = "https://nva.sikt.no/ontology/publication#source";
    private final String framedJson;
    private final RawContentRetriever uriRetriever;

    public FramedJsonGenerator(List<InputStream> streams, Document frame, RawContentRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
        var model = createModel(streams);
        framedJson = frameJsonLd(model, frame);
    }

    public String getFramedJson() {
        return framedJson;
    }

    private static void logInvalidJsonLdInput(Exception exception) {
        logger.warn("Invalid JSON LD input encountered: ", exception);
    }

    private Model createModel(List<InputStream> streams) {
        var model = ModelFactory.createDefaultModel();
        streams.forEach(s -> loadDataIntoModel(model, s));
        addTopLevelOrganizations(model);
        addContributorOrganizations(model);
        addSubUnitsToTopLevelAffiliation(model);
        model.add(constructFundingsFromProjects(model));
        return model;
    }

    private Model constructFundingsFromProjects(Model model) {
        var projectsModel = assembleProjectData(model);

        try (var qexec = QueryExecutionFactory.create(constructFundingsQuery(model), projectsModel)) {
            return qexec.execConstruct();
        }
    }

    private Model assembleProjectData(Model model) {
        var projectsModel = ModelFactory.createDefaultModel();
        projectsModel.add(model);

        fetchDataFromModelResource(model, ResourceFactory.createProperty(SOURCE_PROPERTY_URI))
            .map(entry -> addFailoverStream(entry, FundingSource.withId(entry.uri()).toJsonString()))
            .forEach(stream -> loadDataIntoModel(model, stream));

        fetchDataFromModelResource(model, ResourceFactory.createProperty(PROJECT_PROPERTY_URI))
            .forEach(entry -> loadDataIntoModel(projectsModel, entry.stream()));

        return projectsModel;
    }

    private static ByteArrayInputStream addFailoverStream(URIStreamEntry entry, String failover) {
        if (isNull(entry.stream())) {
            return new ByteArrayInputStream(failover.getBytes(StandardCharsets.UTF_8));
        } else {
            return entry.stream();
        }
    }

    private record URIStreamEntry(URI uri, ByteArrayInputStream stream) {}

    private Stream<URIStreamEntry> fetchDataFromModelResource(Model model, Property property) {
        return model.listObjectsOfProperty(property)
                   .toList()
                   .stream()
                   .filter(RDFNode::isURIResource)
                   .map(RDFNode::asResource)
                   .map(Resource::getURI)
                   .map(URI::create)
                   .map(this::fetchResponseUriStreamEntry);
    }

    private URIStreamEntry fetchResponseUriStreamEntry(URI uri) {
        var response = uriRetriever.fetchResponse(uri, APPLICATION_JSON_LD.toString());
        ByteArrayInputStream data = null;
        if (response.isPresent() && response.get().statusCode() == SC_OK) {
            data = new ByteArrayInputStream(response.get().body().getBytes(StandardCharsets.UTF_8));
        }
        return new URIStreamEntry(uri, data);
    }

    private static Query constructFundingsQuery(Model model) {
        var fundingsNode = model.listSubjectsWithProperty(RDF.type,
                                                          ResourceFactory.createResource(PUBLICATION_CLASS_URI))
                               .nextResource()
                               .getURI();
        var query = """
            PREFIX nva: <https://nva.sikt.no/ontology/publication#>
            PREFIX project: <https://example.org/project-ontology.ttl#>
            
            CONSTRUCT {
              <%s> nva:funding ?funding .
              ?funding a ?type ;
                  nva:source ?source ;
                  nva:identifier ?identifier ;
                  nva:label ?label .
            } WHERE {
              [] project:funding ?funding .
              ?funding a ?rawType ;
                  project:source ?source ;
                  project:identifier ?identifier ;
                  project:label ?label .
              BIND(IRI(REPLACE(STR(?rawType), STR(project:), STR(nva:))) AS ?type)
            
              FILTER NOT EXISTS {
                  ?publication a nva:Publication ;
                    nva:funding ?publicationFunding .
                  ?publicationFunding nva:source ?publicationFundingSource ;
                    nva:identifier ?publicationFundingIdentifier ;
                    a ?publicationFundingType .
                  FILTER(
                    STR(?publicationFunding) != STR(?funding)
                    && ?publicationFundingSource = ?source
                    && ?publicationFundingIdentifier = ?identifier
                    && ?publicationFundingType = ?type
                  )
              }
            }
            """.formatted(fundingsNode);

        return QueryFactory.create(query);
    }

    private void loadDataIntoModel(Model model, InputStream inputStream) {
        if (isNull(inputStream)) {
            return;
        }
        try {
            RDFDataMgr.read(model, inputStream, Lang.JSONLD);
        } catch (RiotException e) {
            logInvalidJsonLdInput(e);
        }
    }

    private void addTopLevelOrganizations(Model model) {
        var query = AffiliationQueries.TOP_LEVEL_ORGANIZATION;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var topLevelNode = qexec.execConstruct();
            model.add(topLevelNode);
        }
    }

    private void addContributorOrganizations(Model model) {
        var query = AffiliationQueries.CONTRIBUTOR_ORGANIZATION;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var organizations = qexec.execConstruct();
            model.add(organizations);
        }
    }

    private Model addSubUnitsToTopLevelAffiliation(Model model) {
        var query = AffiliationQueries.HAS_PART;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var hasPartNodes = qexec.execConstruct();
            return model.add(hasPartNodes);
        }
    }
}
