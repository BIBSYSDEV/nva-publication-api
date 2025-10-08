package no.unit.nva.expansion.utils;

import static java.util.Objects.isNull;
import static no.unit.nva.expansion.utils.JsonLdDefaults.frameJsonLd;
import static org.apache.http.HttpStatus.SC_OK;
import com.apicatalog.jsonld.document.Document;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
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
    private static final String PROJECT_SOURCE_URI = "https://example.org/project-ontology.ttl#source";
    private static final Path FUNDING_QUERY_FILE_PATH = Path.of("funding_query.sparql");
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
        addContributorPreviewAndCount(model);
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

        var tempModel = ModelFactory.createDefaultModel();
        Stream.of(PROJECT_PROPERTY_URI, SOURCE_PROPERTY_URI, PROJECT_SOURCE_URI)
            .forEach(uri -> {
                tempModel.removeAll();
                fetchDataFromModelResource(projectsModel, ResourceFactory.createProperty(uri))
                    .forEach(stream -> loadDataIntoModel(tempModel, stream));
                projectsModel.add(tempModel);
            });
        return projectsModel;
    }

    private Stream<InputStream> fetchDataFromModelResource(Model model, Property property) {
        return model.listObjectsOfProperty(property)
                   .toList()
                   .stream()
                   .filter(RDFNode::isURIResource)
                   .map(RDFNode::asResource)
                   .map(Resource::getURI)
                   .map(URI::create)
                   .map(a -> uriRetriever.fetchResponse(a, MediaTypes.APPLICATION_JSON_LD.toString()))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .filter(a -> a.statusCode() == SC_OK)
                   .map(HttpResponse::body)
                   .map(body -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    private static Query constructFundingsQuery(Model model) {
        var publicationUri = model.listSubjectsWithProperty(RDF.type,
                                                            ResourceFactory.createResource(PUBLICATION_CLASS_URI))
                                 .nextResource()
                                 .getURI();

        var query = IoUtils.stringFromResources(FUNDING_QUERY_FILE_PATH).formatted(publicationUri);

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

    private Model addContributorPreviewAndCount(Model model) {
        var query = """
            PREFIX  nva: <https://nva.sikt.no/ontology/publication#>

            CONSTRUCT {
              ?entityDescription nva:contributorsPreview ?contributorsPreview ;
                                 nva:contributorsCount ?contributorsCount .
            } WHERE {
              {
                SELECT (COUNT(?contributor) AS ?contributorsCount) WHERE {
                  ?publication a nva:Publication ;
                               nva:entityDescription ?entityDescription .
                  ?entityDescription nva:contributor ?contributor .
                }
              }
              {
                SELECT ?contributorsPreview ?entityDescription WHERE {
                  ?publication a nva:Publication ;
                               nva:entityDescription ?entityDescription .
                  ?entityDescription nva:contributor ?contributorsPreview .
                  ?contributorsPreview nva:sequence ?sequence .
                } ORDER BY ASC(?sequence) LIMIT 10
              }
            }
            """;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var contributorPreview = qexec.execConstruct();
            return model.add(contributorPreview);
        }
    }

}
