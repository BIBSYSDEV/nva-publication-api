package no.unit.nva;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OntologyTest {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
    private static final URI BASE_URI = URI.create("https://localhost");
    public static final JsonNode JSON_LD_CONTEXT =
        attempt(() -> MAPPER.readTree(Publication.getJsonLdContext(BASE_URI))).orElseThrow();
    public static final String ONTOLOGY_STRING = Publication.getOntology(BASE_URI);
    public static final SimpleSelector ANY_CLASS_SELECTOR = new SimpleSelector(null, RDF.type, (RDFNode) null);
    public static final SimpleSelector ANY_STATEMENT_SELECTOR = new SimpleSelector(null, null, (RDFNode) null);
    public static final SimpleSelector ONTOLOGY_CLASS_SELECTOR = new SimpleSelector(null, RDF.type, RDFS.Class);
    public static final SimpleSelector ONTOLOGY_PROPERTY_SELECTOR = new SimpleSelector(null, RDF.type, RDF.Property);

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    public static Stream<Arguments> modelPropertiesProvider() {
        return Stream.of(Arguments.of(getModelProperties().toArray()));
    }

    public static Stream<Arguments> modelClassProvider() {
        return Stream.of(Arguments.of(getModelClasses().toArray()));
    }

    @Test
    void shouldContainDistinctDescriptions() {
        var ontologyValues = ONTOLOGY_STRING.lines()
                                 .filter(line -> line.startsWith("nva:"))
                                 .collect(Collectors.toList());
        var distinctValues = ontologyValues.stream().distinct().collect(Collectors.toList());
        String duplicatesMessage = ontologyValues.equals(distinctValues) ? null : getDuplicatesMessage(ontologyValues);
        assertThat(duplicatesMessage, distinctValues, is(equalTo(ontologyValues)));
    }

    @ParameterizedTest
    @MethodSource("modelClassProvider")
    void shouldContainEveryVisibleClassOfModel(String modelClass) {
        var ontologyClasses = extractClassesFromOntology();
        assertThat(ontologyClasses, hasItem(modelClass));
    }

    @ParameterizedTest
    @MethodSource("modelPropertiesProvider")
    void shouldContainEveryVisiblePropertyOfModel(String modelProperty) {
        var ontologyProperties = extractPropertiesFromOntology();
        assertThat(ontologyProperties, hasItem(modelProperty));
    }

    private static String getDuplicatesMessage(List<String> ontologyValues) {
        // Not a performant solution, with Collections.frequency, but here it is not important
        var duplicates = ontologyValues.stream()
                             .filter(e -> Collections.frequency(ontologyValues, e) > 1)
                             .distinct()
                             .collect(Collectors.joining(", "));
        return "Duplicates found: " + duplicates;
    }

    private static Set<String> getModelClasses() {
        return createModelFromJson(generateAllNvaTypes()).listStatements(ANY_CLASS_SELECTOR).toSet().stream()
                   .map(Statement::getObject)
                   .map(RDFNode::asResource)
                   .map(Resource::getLocalName)
                   .collect(Collectors.toSet());
    }

    private static Set<String> getModelProperties() {
        return createModelFromJson(generateAllNvaTypes()).listStatements(ANY_STATEMENT_SELECTOR).toSet().stream()
                   .map(Statement::getPredicate)
                   .filter(OntologyTest::isNotRdfType)
                   .map(Resource::getURI)
                   .collect(Collectors.toSet());
    }

    private static boolean isNotRdfType(Property i) {
        return !i.equals(RDF.type);
    }

    private static List<InputStream> generateAllNvaTypes() {
        return publicationInstanceProvider().map(PublicationGenerator::randomPublication)
                   .map(OntologyTest::serializeToJson)
                   .map(OntologyTest::addContextObject)
                   .map(OntologyTest::toByteArrayInputStream)
                   .collect(Collectors.toList());
    }

    private static ByteArrayInputStream toByteArrayInputStream(String item) {
        return new ByteArrayInputStream(item.getBytes(StandardCharsets.UTF_8));
    }

    private static String addContextObject(String string) {
        var jsonNode = (ObjectNode) attempt(() -> MAPPER.readTree(string)).orElseThrow();
        jsonNode.set("@context", JSON_LD_CONTEXT);
        return attempt(() -> MAPPER.writeValueAsString(jsonNode)).orElseThrow();
    }

    private static Model createModelFromJson(List<InputStream> inputStreams) {
        var model = ModelFactory.createDefaultModel();
        inputStreams.forEach(item -> RDFDataMgr.read(model, item, Lang.JSONLD11));
        return model;
    }

    private static String serializeToJson(Publication object) {
        return attempt(() -> MAPPER.writeValueAsString(object)).orElseThrow();
    }

    private List<String> extractClassesFromOntology() {
        var model = getOntologyModel();
        return model.listStatements(ONTOLOGY_CLASS_SELECTOR).toSet().stream()
                   .map(Statement::getSubject)
                   .map(Resource::getLocalName)
                   .collect(Collectors.toList());
    }

    private List<String> extractPropertiesFromOntology() {
        return getOntologyModel().listStatements(ONTOLOGY_PROPERTY_SELECTOR).toSet().stream()
                   .map(Statement::getSubject)
                   .map(Resource::getURI)
                   .distinct()
                   .collect(Collectors.toList());
    }

    private Model getOntologyModel() {
        var model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, new ByteArrayInputStream(ONTOLOGY_STRING.getBytes(StandardCharsets.UTF_8)), Lang.TURTLE);
        return model;
    }
}
