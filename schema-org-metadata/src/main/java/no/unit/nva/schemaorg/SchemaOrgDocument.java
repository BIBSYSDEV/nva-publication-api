package no.unit.nva.schemaorg;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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
        var byteArrayOutputStream = new ByteArrayOutputStream();
        RDFDataMgr.write(byteArrayOutputStream, result, Lang.JSONLD);
        try (var inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            var jsonDocument = JsonDocument.of(MediaType.JSON_LD, inputStream);
            var frame = createFrame(extractTypeForFrame(result));
            return write(JsonLd.frame(jsonDocument, frame).get());
        } catch (IOException | JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    private static String write(JsonObject frame) {
        var stringWriter = new StringWriter();
        try (var jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.write(frame);
        }
        return stringWriter.toString();
    }

    private static JsonDocument createFrame(String type) {
        var frame = new ByteArrayInputStream(String.format(JSON_LD_FRAME_TEMPLATE, type)
                                                 .getBytes(StandardCharsets.UTF_8));
        try {
            return JsonDocument.of(frame);
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }
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
