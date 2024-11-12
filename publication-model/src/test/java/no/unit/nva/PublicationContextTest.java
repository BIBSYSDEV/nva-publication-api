package no.unit.nva;

import static java.util.Collections.emptyList;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.NullPublisher;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PublicationContextTest {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
    public static final Set<String> SET_DEFINED_IN_JSON_LD_CONTEXT = getAllContextContainerSetTerms();

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    private static Stream<? extends PublishingHouse> nullPublishingHouseProvider() {
        return Stream.of(null,
                         new Publisher(null),
                         new UnconfirmedPublisher(null),
                         new NullPublisher());
    }

    @Test
    void shouldReturnContextObjectWhenRequested() {
        assertThat(Publication.getJsonLdContext(
            UriWrapper.fromHost(new Environment().readEnv("API_HOST")).getUri()),
                   is(not(nullValue())));
    }

    @Test
    void shouldNotSerializeTheJsonLdContextObjectFromTheClass() {
        var serialized = attempt(() -> MAPPER.writeValueAsString(new Publication())).orElseThrow();
        assertThat(serialized, not(containsString("jsonLdContext")));
    }

    // TODO: actually filter the expected classes from ontology
    @Test
    void shouldWriteEveryNvaClassAsFragmentUri() {
        var objectResources = generateListOfDistinctObjectClassesForEveryNvaPublicationType();
        var nvaObjects = objectResources.stream()
                             .filter(PublicationContextTest::isAnNvaObjectResource)
                             .filter(PublicationContextTest::isNonFragmentUri)
                             .toList();
        assertThat(nvaObjects, is(emptyList()));
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    void shouldContainEveryCollectionInModel(Class<?> instanceType) {
        var allNvaCollectionProperties = generateSetOfPropertiesThatHaveCollectionTypeForEveryNvaType(instanceType);
        for (var modelProperty : allNvaCollectionProperties) {
            assertTrue(SET_DEFINED_IN_JSON_LD_CONTEXT.contains(modelProperty));
        }
    }

    @ParameterizedTest
    @MethodSource("nullPublishingHouseProvider")
    void shouldReturnNullPublisherWhenResearchDataEffectivelyHasNoPublisher(PublishingHouse publishingHouse) {
        var researchData = new ResearchData(publishingHouse);
        assertThat(researchData.getPublisher(), instanceOf(NullPublisher.class));
    }

    // TODO: test that every property and class is described in the ontology

    private static Set<String> getAllContextContainerSetTerms() {
        var elements = getElementsFromJsonContextObject();
        return extractPropertyKeysThatSpecifyContainerSets(elements);
    }

    private static Set<String> extractPropertyKeysThatSpecifyContainerSets(
        Iterator<Map.Entry<String, JsonNode>> elements) {
        return streamOf(elements)
                   .filter(PublicationContextTest::isContainerSetSpecification)
                   .map(Map.Entry::getKey)
                   .collect(Collectors.toSet());
    }

    private static Iterator<Map.Entry<String, JsonNode>> getElementsFromJsonContextObject() {
        var contextFromPublication = Publication.getJsonLdContext(
            UriWrapper.fromHost(new Environment().readEnv("API_HOST")).getUri());
        var jsonLdContext = stringToTree(contextFromPublication);
        return jsonLdContext.fields();
    }

    private static boolean isContainerSetSpecification(Map.Entry<String, JsonNode> current) {
        return current.getValue().isObject()
               && current.getValue().has("@container")
               && current.getValue().get("@container").asText().equals("@set");
    }

    private Set<String> generateSetOfPropertiesThatHaveCollectionTypeForEveryNvaType(Class<?> instanceType) {
        var publication = PublicationGenerator.randomPublication(instanceType);
        var jsonNode = PublicationContextTest.convertToJsonNode(publication);
        return initiateArrayFieldNameExtraction(jsonNode);
    }

    private static Set<String> initiateArrayFieldNameExtraction(JsonNode jsonNode) {
        return JsonFlattener.getFlattenedKeysAndTypes(jsonNode);
    }

    private static Set<String> generateListOfDistinctObjectClassesForEveryNvaPublicationType() {
        return generateStatementsForEveryPublicationType().stream()
                   .map(Statement::getObject)
                   .filter(PublicationContextTest::isFullyQualifiedUri)
                   .map(RDFNode::asResource)
                   .map(Resource::getURI)
                   .collect(Collectors.toSet());
    }

    private static boolean isFullyQualifiedUri(RDFNode object) {
        return object.isResource() && !object.asResource().isAnon();
    }

    private static Set<Statement> generateStatementsForEveryPublicationType() {
        var publications = publicationInstanceProvider()
                               .map(PublicationContextTest::generatePublicationWithContext)
                               .toList();
        return getDistinctStatementsOfResources(publications);
    }

    private static Set<Statement> getDistinctStatementsOfResources(List<InputStream> inputStreams) {
        var model = ModelFactory.createDefaultModel();
        inputStreams.forEach(inputStream -> RDFDataMgr.read(model, inputStream, Lang.JSONLD11));
        return model.listStatements().toSet();
    }

    private static boolean isNonFragmentUri(String item) {
        return !item.contains("#");
    }

    private static boolean isAnNvaObjectResource(String item) {
        return item.startsWith("https://nva.sikt.no/");
    }

    private static InputStream generatePublicationWithContext(Class<?> type) {
        var publication = PublicationGenerator.randomPublication(type);
        return new ByteArrayInputStream((addContextObjectToPublication(publication)));
    }

    private static byte[] addContextObjectToPublication(Publication publication) {
        var jsonNode = (ObjectNode) convertToJsonNode(publication);
        jsonNode.set("@context", stringToTree(
            Publication.getJsonLdContext(UriWrapper.fromHost(new Environment().readEnv("API_HOST")).getUri())));
        return jsonNode.toString().getBytes();
    }

    private static JsonNode stringToTree(String string) {
        return attempt(() -> MAPPER.readTree(string)).orElseThrow();
    }

    private static JsonNode convertToJsonNode(Publication instance) {
        return (JsonNode) attempt(() -> MAPPER.valueToTree(instance)).orElseThrow();
    }

    private static <T> Stream<T> streamOf(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }



    public static class JsonFlattener {

        public static Set<String> getFlattenedKeysAndTypes(JsonNode jsonNode) {
            return getFlattenedKeysAndTypes(jsonNode, "");
        }

        private static Set<String> getFlattenedKeysAndTypes(JsonNode jsonNode, String currentPath) {
            var keyList = new HashSet<String>();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> jsonField = fields.next();
                String keyName = currentPath.isEmpty() ? jsonField.getKey() : currentPath + "." + jsonField.getKey();
                JsonNode fieldValue = jsonField.getValue();

                if (fieldValue.isObject()) {
                    keyList.addAll(getFlattenedKeysAndTypes(fieldValue, keyName));
                } else if (fieldValue.isArray()) {
                    keyList.add(jsonField.getKey());
                }
            }
            return keyList;
        }
    }
}
