package no.unit.nva.expansion.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdOptions;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;

@JacocoGenerated
public class FramedJsonGenerator {

    public static final String JSON_LD_GRAPH = "@graph";
    private static final Logger logger = LoggerFactory.getLogger(FramedJsonGenerator.class);
    private final Model model;
    private final String framedJson;

    public FramedJsonGenerator(List<InputStream> streams, String frame) {
        model = createModel(streams);
        framedJson = getFramedModelJson(frame);
    }

    public String getFramedJson() {
        return framedJson;
    }

    private Model createModel(List<InputStream> streams) {
        ObjectNode document = objectMapper.createObjectNode();
        ArrayNode graph = objectMapper.createArrayNode();
        streams.stream()
                .map(attempt(objectMapper::readTree))
                .filter(this::keepSuccessesAndLogErrors)
                .map(Try::orElseThrow)
                .forEach(graph::add);

        document.set(JSON_LD_GRAPH, graph);

        var defaultModel = ModelFactory.createDefaultModel();
        updateModel(document.toString(), defaultModel);
        return addTopLevelAffiliation(defaultModel);
    }

    private static void updateModel(String update, Model oldModel) {
        RDFDataMgr.read(oldModel, new ByteArrayInputStream(update.getBytes(StandardCharsets.UTF_8)), Lang.JSONLD);
    }

    private Model addTopLevelAffiliation(Model model) {

        var query = AffiliationQueries.TOP_LEVEL_AFFILIATION;
        QueryFactory.create(query);

        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var topLevelNode = qexec.execConstruct();
            return model.union(topLevelNode);
        }
    }

    private String getFramedModelJson(String frame) {
        return RDFWriter.create()
                .format(RDFFormat.JSONLD10_FRAME_PRETTY)
                .context(getJsonLdWriteContext(frame))
                .source(model)
                .build()
                .asString();
    }

    private static JsonLDWriteContext getJsonLdWriteContext(String frame) {
        var context = new JsonLDWriteContext();
        context.setOptions(getJsonLdOptions());
        context.setFrame(frame);
        return context;
    }

    private static JsonLdOptions getJsonLdOptions() {
        var jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setOmitDefault(true);
        jsonLdOptions.setUseNativeTypes(true);
        jsonLdOptions.setPruneBlankNodeIdentifiers(true);
        return jsonLdOptions;
    }

    private boolean keepSuccessesAndLogErrors(Try<JsonNode> jsonNodeTry) {
        if (jsonNodeTry.isFailure()) {
            logFramingFailure(jsonNodeTry.getException());
        }
        return jsonNodeTry.isSuccess();
    }

    private void logFramingFailure(Exception exception) {
        logger.warn("Framing failed:", exception);
    }
}
