package no.unit.nva.expansion.utils;

import static java.util.Objects.isNull;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jsonldjava.core.JsonLdOptions;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import no.unit.nva.expansion.model.ExpandedResource;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
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

public final class NviCalculator {

    private static final String NVI_YEAR = "2023";

    private static final String NVI_YEAR_REPLACE_STRING = "__NVI_YEAR__";

    private static final String NVI_TYPE =
        IoUtils.stringFromResources(Path.of("nviTypeQuery.sparql")).replace(NVI_YEAR_REPLACE_STRING, NVI_YEAR);
    private static final Logger logger = LoggerFactory.getLogger(NviCalculator.class);

    private static final String FRAME_SRC = IoUtils.stringFromResources(Path.of("nviFrame.json"));

    private NviCalculator() {
    }

    public static ExpandedResource calculateNviType(ExpandedResource expandedResource) throws JsonProcessingException {
        var model = createModelWithNviType(expandedResource);
        var expandedResourceJsonWithNviType = getFramedModelJson(model);
        return attempt(
            () -> objectMapper.readValue(expandedResourceJsonWithNviType, ExpandedResource.class)).orElseThrow();
    }

    private static Model createModelWithNviType(ExpandedResource expandedResource) throws JsonProcessingException {
        var inputStreams = List.of(stringToStream(objectMapper.writeValueAsString(expandedResource)));
        var model = createModel(inputStreams);
        addNviType(model);
        return model;
    }

    private static Model createModel(List<InputStream> inputStreams) {
        var model = ModelFactory.createDefaultModel();
        inputStreams.forEach(s -> loadDataIntoModel(model, s));
        return model;
    }

    private static void addNviType(Model model) {
        try (var qexec = QueryExecutionFactory.create(NVI_TYPE, model)) {
            var nviType = qexec.execConstruct();
            model.add(nviType);
        }
    }

    private static String getFramedModelJson(Model model) {
        return RDFWriter.create()
                   .format(RDFFormat.JSONLD10_FRAME_PRETTY)
                   .context(getJsonLdWriteContext())
                   .source(model)
                   .build()
                   .asString();
    }

    private static JsonLDWriteContext getJsonLdWriteContext() {
        var context = new JsonLDWriteContext();
        context.setOptions(getJsonLdOptions());
        context.setFrame(FRAME_SRC);
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

    @JacocoGenerated
    private static void loadDataIntoModel(Model model, InputStream inputStream) {
        if (isNull(inputStream)) {
            return;
        }
        try {
            RDFDataMgr.read(model, inputStream, Lang.JSONLD);
        } catch (RiotException e) {
            logInvalidJsonLdInput(e);
        }
    }

    @JacocoGenerated
    private static void logInvalidJsonLdInput(Exception exception) {
        logger.warn("Invalid JSON LD input encountered: ", exception);
    }
}