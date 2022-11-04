package no.unit.nva.schemaorg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import nva.commons.core.SingletonCollector;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

public final class SchemaOrgDocument {
    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
    public static final ByteArrayInputStream ONTOLOGY_MAPPINGS =
            new ByteArrayInputStream(stringFromResources(Path.of("subtype_mappings.ttl"))
                    .getBytes(StandardCharsets.UTF_8));
    public static final String QUERY = stringFromResources(Path.of("schema_org_conversion.sparql"));
    public static final String JSON_LD_FRAME_TEMPLATE = stringFromResources(Path.of("json_ld_frame.json"));
    public static final Query CONSTRUCT_SCHEMA_VIEW_QUERY =
            QueryFactory.create(stringFromResources(Path.of("type_selector.sparql")));
    public static final String EMPTY_JSON_OBJECT = "{}";
    private final PublicationResponse publication;

    public SchemaOrgDocument(Publication publication) {
        this.publication = PublicationMapper.convertValue(publication, PublicationResponse.class);
    }

    private Model extractSchemaView() {
        var query = QueryFactory.create(QUERY);
        try (var queryExecution = QueryExecutionFactory.create(query, getModelWithMappings())) {
            return queryExecution.execConstruct();
        }
    }

    private Model getModelWithMappings() {
        var model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, toInputStream(publication), Lang.JSONLD);
        RDFDataMgr.read(model, ONTOLOGY_MAPPINGS, Lang.TURTLE);
        return model;
    }

    private ByteArrayInputStream toInputStream(PublicationResponse publication) {
        var x = attempt(() -> MAPPER.writeValueAsString(publication));
        return x.map(String::getBytes)
                .map(ByteArrayInputStream::new)
                .orElseThrow();
    }

    private String getJsonLdStringOfModel(Model result) {
        return RDFWriter.create()
                .format(RDFFormat.JSONLD10_FRAME_PRETTY)
                .context(getJsonLdWriteContext(extractTypeForFrame(result)))
                .source(result)
                .build()
                .asString();
    }

    private JsonLDWriteContext getJsonLdWriteContext(String type) {
        var context = new JsonLDWriteContext();
        context.setOptions(getJsonLdOptions());
        var frame = String.format(JSON_LD_FRAME_TEMPLATE, type);
        context.setFrame(frame);
        return context;
    }

    private JsonLdOptions getJsonLdOptions() {
        var jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setPruneBlankNodeIdentifiers(true);
        return jsonLdOptions;
    }

    private String extractTypeForFrame(Model model) {
        try (var queryExecution = QueryExecutionFactory.create(CONSTRUCT_SCHEMA_VIEW_QUERY, model)) {
            var results = queryExecution.execSelect();
            var queryParameter = results.getResultVars().stream().collect(SingletonCollector.collect());
            return results.next().get(queryParameter).asResource().getLocalName();
        }
    }

    public String getRepresentation() {
        var input = extractSchemaView();
        return !input.isEmpty() ? getJsonLdStringOfModel(input) : EMPTY_JSON_OBJECT;
    }
}
