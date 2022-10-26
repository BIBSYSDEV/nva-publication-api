package no.unit.nva.schemaorg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedResource;
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
    public static final Query QUERY =
            QueryFactory.create(stringFromResources(Path.of("schema_org_conversion.sparql")));
    public static final String JSON_LD_FRAME_TEMPLATE = stringFromResources(Path.of("json_ld_frame.json"));
    public static final Query CONSTRUCT_SCHEMA_VIEW_QUERY =
            QueryFactory.create(stringFromResources(Path.of("type_selector.sparql")));

    private SchemaOrgDocument() {
        // NO-OP
    }

    public static String fromExpandedResource(ExpandedResource resource) {
        var input = extractSchemaView(resource);
        return getJsonLdStringOfModel(input);
    }

    private static Model extractSchemaView(ExpandedResource resource) {
        try (var queryExecution = QueryExecutionFactory.create(QUERY, getModelWithMappings(resource))) {
            return queryExecution.execConstruct();
        }
    }

    private static Model getModelWithMappings(ExpandedResource resource) {
        var model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, toInputStream(resource), Lang.JSONLD);
        RDFDataMgr.read(model, ONTOLOGY_MAPPINGS, Lang.TURTLE);
        return model;
    }

    private static ByteArrayInputStream toInputStream(ExpandedResource resource) {
        return attempt(() -> MAPPER.writeValueAsString(resource))
                .map(String::getBytes)
                .map(ByteArrayInputStream::new)
                .orElseThrow();
    }

    private static String getJsonLdStringOfModel(Model result) {
        return RDFWriter.create()
                .format(RDFFormat.JSONLD10_FRAME_PRETTY)
                .context(getJsonLDWriteContext(extractTypeForFrame(result)))
                .source(result)
                .build()
                .asString();
    }

    private static JsonLDWriteContext getJsonLDWriteContext(String type) {
        var context = new JsonLDWriteContext();
        context.setOptions(getJsonLdOptions());
        var frame = String.format(JSON_LD_FRAME_TEMPLATE, type);
        context.setFrame(frame);
        return context;
    }

    private static JsonLdOptions getJsonLdOptions() {
        var jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setPruneBlankNodeIdentifiers(true);
        return jsonLdOptions;
    }

    private static String extractTypeForFrame(Model model) {
        try (var queryExecution = QueryExecutionFactory.create(CONSTRUCT_SCHEMA_VIEW_QUERY, model)) {
            var results = queryExecution.execSelect();
            var queryParameter = results.getResultVars().stream().collect(SingletonCollector.collect());
            return results.next().get(queryParameter).asResource().getLocalName();
        }
    }
}
