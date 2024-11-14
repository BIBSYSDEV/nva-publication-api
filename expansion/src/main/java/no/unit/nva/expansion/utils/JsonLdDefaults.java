package no.unit.nva.expansion.utils;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

public final class JsonLdDefaults {

    private JsonLdDefaults() {

    }

    public static String frameJsonLd(Model model, Document frame) {
        var outputStream = new ByteArrayOutputStream();
        RDFDataMgr.write(outputStream, model, RDFFormat.JSONLD);
        try (var inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            var jsonDocument = JsonDocument.of(MediaType.JSON_LD, inputStream);
            return write(JsonLd.frame(jsonDocument, frame).options(getJsonLdOptions()).get());
        } catch (IOException | JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    private static String write(JsonObject framedObject) {
        var stringWriter = new StringWriter();
        try (var jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.write(framedObject);
        }
        return stringWriter.toString();
    }

    private static JsonLdOptions getJsonLdOptions() {
        var jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setOmitDefault(true);
        jsonLdOptions.setUseNativeTypes(true);
        return jsonLdOptions;
    }
}