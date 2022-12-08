package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.model.ExpandedResource.extractPublicationContextUris;
import static no.unit.nva.expansion.utils.JsonLdUtils.toJsonString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.core.JsonLdOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.expansion.utils.FramedJsonGenerator;
import no.unit.nva.expansion.utils.SearchIndexFrame;
import no.unit.nva.expansion.utils.UriRetriever;
import nva.commons.core.ioutils.IoUtils;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;

public class IndexDocumentWrapperLinkedData {

    private final UriRetriever uriRetriever;

    public IndexDocumentWrapperLinkedData(UriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String toFramedJsonLd(JsonNode indexDocument) throws IOException {
        String frame = SearchIndexFrame.FRAME_SRC;
        List<InputStream> inputStreams = getInputStreams(indexDocument);
        return new FramedJsonGenerator(inputStreams, stringToStream(frame)).getFramedJson();
    }

    public String updateDataWithInverseRelations() {

        var lowLevelOrg = "{\n"
                          + "    \"type\": \"Organization\",\n"
                          + "    \"@context\": { \"@vocab\": \"https://example.org/vocab#\","
                          + "    \"id\": \"@id\",\n"
                          + "    \"type\": \"@type\","
                          + "    \"name\": {"
                          + "      \"@container\": \"@language\""
                          + "    }},"
                          + "    \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.60.0.0\",\n"
                          + "    \"name\": {\n"
                          + "        \"nb\": \"Avdeling for økonomi, språk og samfunnsfag\"\n"
                          + "    },\n"
                          + "    \"partOf\": [\n"
                          + "        {\n"
                          + "            \"type\": \"Organization\",\n"
                          + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.0.0"
                          + ".0\",\n"
                          + "            \"name\": {\n"
                          + "                \"en\": \"Østfold University College\",\n"
                          + "                \"nb\": \"Høgskolen i Østfold\"\n"
                          + "            }\n"
                          + "        }\n"
                          + "    ]\n"
                          + "}";

        var moreOrgs = "{\n"
                       + "    \"type\": \"Organization\",\n"
                       + "    \"@context\": { \"@vocab\": \"https://example.org/vocab#\","
                       + "    \"id\": \"@id\",\n"
                       + "    \"type\": \"@type\","
                       + "    \"name\": {"
                       + "      \"@container\": \"@language\""
                       + "    }},"
                       + "    \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.0.0.0\",\n"
                       + "    \"name\": {\n"
                       + "        \"en\": \"Østfold University College\",\n"
                       + "        \"nb\": \"Høgskolen i Østfold\"\n"
                       + "    },\n"
                       + "    \"hasPart\": [\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.90.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Fremmedspråksenteret\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.26.0.10\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Bibliotek\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.20.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Fellestjenesten\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.90.0.10\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"NSF - Fremmedspråksenteret\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.30.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Avdeling for lærerutdanning\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.55.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Avdeling for informasjonsteknologi\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.50.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Avdeling for ingeniørfag\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.40.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Avdeling for helse og velferd\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.80.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"HiØ VIDERE\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.26.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Bibliotek\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.70.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Akademi for scenekunst\"\n"
                       + "            }\n"
                       + "        },\n"
                       + "        {\n"
                       + "            \"type\": \"Organization\",\n"
                       + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/224.60.0.0\",\n"
                       + "            \"name\": {\n"
                       + "                \"nb\": \"Avdeling for økonomi, språk og samfunnsfag\"\n"
                       + "            }\n"
                       + "        }\n"
                       + "    ]\n"
                       + "}";

        var model = ModelFactory.createDefaultModel();
        updateModel(lowLevelOrg, model);
        var initialId = extractInitialId(model);
        updateModel(moreOrgs, model);

        var queryString = "PREFIX  organization: <https://example.org/vocab#>"
                          + "\n"
                          + "CONSTRUCT {?b organization:partOf ?a } WHERE { ?a organization:hasPart ?b }";
        var query = QueryFactory.create(queryString);

        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var result = qexec.execConstruct();
            model.add(result);
        }
        return getJsonLdStringOfModel(moreModelStuff(model, initialId));
    }

    private String extractInitialId(Model model) {
        var queryString = "PREFIX  organization: <https://example.org/vocab#>"
                          + "SELECT ?a WHERE  { ?a a organization:Organization ."
                          + "  OPTIONAL { ?a organization:partOf ?b }"
                          + "  FILTER(bound(?b))"
                          + "}";
        var query = QueryFactory.create(queryString);
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var results = qexec.execSelect();
            return results.next().get("?a").asResource().getURI();
        }
    }

    private Model moreModelStuff(Model model, String initialId) {
        var queryString = "PREFIX  organization: <https://example.org/vocab#>"
                          + "\n"
                          + "CONSTRUCT { "
                          + "?a a organization:Organization ;"
                          + "    organization:name ?c ;"
                          + "    organization:hasPart ?d ."
                          + "?d a organization:Organization ; "
                          + "    organization:name ?f } WHERE { ?a a "
                          + "organization:Organization "
                          + "; "
                          + " organization:name ?c ."
                          + "?a organization:hasPart+ ?d ."
                          + "?d organization:name ?f ."
                          + "FILTER(?d = <" + initialId + ">)}";
        var query = QueryFactory.create(queryString);

        try (var qexec = QueryExecutionFactory.create(query, model)) {
            return qexec.execConstruct();
        }

    }

    private static String getJsonLdStringOfModel(Model result) {
        return RDFWriter.create()
                   .format(RDFFormat.JSONLD10_FRAME_PRETTY)
                   .context(getJsonLdWriteContext())
                   .source(result)
                   .build()
                   .asString();
    }

    private static JsonLDWriteContext getJsonLdWriteContext() {
        var context = new JsonLDWriteContext();
        context.setOptions(getJsonLdOptions());
        context.setFrame(createFrame());
        return context;
    }

    private static String createFrame() {
        return "{\n"
               + "  \"@context\": {\n"
               + "    \"@vocab\": \"https://example.org/vocab#\",\n"
               + "    \"id\": \"@id\",\n"
               + "    \"type\": \"@type\",\n"
               + "    \"name\": {\n"
               + "      \"@container\": \"@language\"\n"
               + "    }\n"
               + "  },\n"
               + "  \"hasPart\": {\n"
               + "    \"@embed\": \"@always\"\n"
               + "  }\n"
               + "}\n";
    }

    private static JsonLdOptions getJsonLdOptions() {
        var jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setOmitDefault(true);
        jsonLdOptions.setPruneBlankNodeIdentifiers(true);
        return jsonLdOptions;
    }

    private static void updateModel(String update, Model model) {
        RDFDataMgr.read(model, new ByteArrayInputStream(update.getBytes(StandardCharsets.UTF_8)), Lang.JSONLD);
    }

    private List<InputStream> getInputStreams(JsonNode indexDocument) {
        final List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(stringToStream(toJsonString(indexDocument)));
        inputStreams.addAll(fetchAll(extractPublicationContextUris(indexDocument)));
        return inputStreams;
    }

    private Collection<? extends InputStream> fetchAll(List<URI> publicationContextUris) {
        List<Optional<String>> uriContent =
            publicationContextUris.stream().map(this::fetch).collect(Collectors.toList());
        return uriContent.stream()
                   .flatMap(Optional::stream)
                   .map(IoUtils::stringToStream)
                   .collect(Collectors.toList());
    }

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }
}
