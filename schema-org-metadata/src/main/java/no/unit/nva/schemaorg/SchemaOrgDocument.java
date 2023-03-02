package no.unit.nva.schemaorg;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import nva.commons.core.SingletonCollector;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchemaOrgDocument {

    public static final Logger logger = LoggerFactory.getLogger(SchemaOrgDocument.class);
    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
    public static final String QUERY = stringFromResources(Path.of("schema_org_conversion.sparql"));
    public static final String JSON_LD_FRAME_TEMPLATE = stringFromResources(Path.of("json_ld_frame.json"));
    public static final String CONSTRUCT_SCHEMA_VIEW_QUERY = stringFromResources(Path.of("type_selector.sparql"));
    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final String MAPPINGS = stringFromResources(Path.of("subtype_mappings.ttl"));

    private SchemaOrgDocument() {
        // NO-OP
    }

    public static String fromPublication(Publication publication) {
        var generatedRepresentation = extractSchemaRepresentation(publication);
        return !generatedRepresentation.isEmpty() ? getJsonLdStringOfModel(generatedRepresentation) : EMPTY_JSON_OBJECT;
    }

    private static Model extractSchemaRepresentation(Publication publication) {
        var query = QueryFactory.create(QUERY);
        try (var queryExecution = QueryExecutionFactory.create(query, getModelWithMappings(publication))) {
            return queryExecution.execConstruct();
        }
    }

    private static Model getModelWithMappings(Publication publication) {
        var publicationResponse = toPublicationResponse(publication);
        var model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, toInputStream(publicationResponse), Lang.JSONLD);
        RDFDataMgr.read(model, loadMappings(), Lang.TURTLE);
        return model;
    }

    private static InputStream loadMappings() {
        return new ByteArrayInputStream(MAPPINGS.getBytes(StandardCharsets.UTF_8));
    }

    private static InputStream toInputStream(PublicationResponse publication) {
        return attempt(() -> MAPPER.writeValueAsBytes(publication)).map(ByteArrayInputStream::new).orElseThrow();
    }

    private static String getJsonLdStringOfModel(Model result) {
        return RDFWriter.create()
                .format(RDFFormat.JSONLD10_FRAME_PRETTY)
                .context(getJsonLdWriteContext(extractTypeForFrame(result)))
                .source(result)
                .build()
                .asString();
    }

    private static JsonLDWriteContext getJsonLdWriteContext(String type) {
        var context = new JsonLDWriteContext();
        context.setOptions(getJsonLdOptions());
        context.setFrame(createFrame(type));
        return context;
    }

    private static String createFrame(String type) {
        return String.format(JSON_LD_FRAME_TEMPLATE, type);
    }

    private static JsonLdOptions getJsonLdOptions() {
        var jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setPruneBlankNodeIdentifiers(true);
        return jsonLdOptions;
    }

    private static String extractTypeForFrame(Model model) {
        var query = QueryFactory.create(CONSTRUCT_SCHEMA_VIEW_QUERY);
        try (var queryExecution = QueryExecutionFactory.create(query, model)) {
            var results = queryExecution.execSelect();
            var queryParameter = results.getResultVars().stream().collect(SingletonCollector.collect());
            return results.next().get(queryParameter).asResource().getLocalName();
        }
    }

    private static PublicationResponse toPublicationResponse(Publication publication) {
        return PublicationMapper.convertValue(publication, PublicationResponse.class);
    }
}
