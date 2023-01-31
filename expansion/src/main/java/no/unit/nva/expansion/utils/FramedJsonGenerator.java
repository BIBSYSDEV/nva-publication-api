package no.unit.nva.expansion.utils;

import static java.util.Objects.isNull;
import com.github.jsonldjava.core.JsonLdOptions;
import java.io.InputStream;
import java.util.List;
import nva.commons.core.JacocoGenerated;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class FramedJsonGenerator {
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
        var m = ModelFactory.createDefaultModel();
        streams.forEach(s -> loadDataIntoModel(m, s));
        return addTopLevelAffiliation(m);
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

    private Model addTopLevelAffiliation(Model model) {

        var query = AffiliationQueries.TOP_LEVEL_AFFILIATION;

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

    private static void logInvalidJsonLdInput(Exception exception) {
        logger.warn("Invalid JSON LD input encountered: ", exception);
    }
}
