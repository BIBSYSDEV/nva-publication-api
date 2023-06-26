package no.unit.nva.expansion.utils;

import com.github.jsonldjava.core.JsonLdOptions;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;

public final class JsonLdDefaults {

    private JsonLdDefaults() {

    }

    public static String frameJsonLd(Model model, String frame) {
        return RDFWriter.create()
                   .format(RDFFormat.JSONLD10_FRAME_PRETTY)
                   .context(getJsonLdWriteContext(frame))
                   .source(model)
                   .build()
                   .asString();
    }

    private static JsonLdOptions getJsonLdOptions() {
        var jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setOmitDefault(true);
        jsonLdOptions.setUseNativeTypes(true);
        jsonLdOptions.setPruneBlankNodeIdentifiers(true);
        return jsonLdOptions;
    }

    private static JsonLDWriteContext getJsonLdWriteContext(String frame) {
        var context = new JsonLDWriteContext();
        context.setOptions(getJsonLdOptions());
        context.setFrame(frame);
        return context;
    }
}