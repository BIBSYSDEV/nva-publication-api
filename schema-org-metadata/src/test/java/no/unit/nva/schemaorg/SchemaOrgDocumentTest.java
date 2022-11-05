package no.unit.nva.schemaorg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.publication.testing.TypeProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.linesfromResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;


class SchemaOrgDocumentTest {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

    public static Stream<Class<?>> instanceTypeProvider() {
        return TypeProvider.listSubTypes(PublicationInstance.class);
    }

    @ParameterizedTest(name = "Should generate schema.org for type: {0}")
    @MethodSource("instanceTypeProvider")
    void shouldReturnSchemaOrgDocumentWithTypeWhenInputIsPublication(Class<? extends PublicationInstance<?>> instance) {
        var publication = randomPublication(instance);
        var actual = SchemaOrgDocument.fromPublication(publication);
        var jsonNode = attempt(() -> MAPPER.readTree(actual)).orElseThrow();
        var expectedType = calculateType(instance);
        assertThatBasicDataIsInPlace(publication, jsonNode, expectedType);
    }

    private List<String> calculateType(Class<? extends PublicationInstance<?>> instance) {
        var name = instance.getSimpleName();
        return linesfromResource(Path.of("subtype_mappings.ttl"))
                .stream()
                .filter(item -> matchRdfPrefixedTerm(name, item))
                .map(this::getSchemaTypeFromString)
                .map(SchemaOrgDocumentTest::splitOnComma)
                .flatMap(Arrays::stream)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static boolean matchRdfPrefixedTerm(String name, String item) {
        return item.contains(":" + name + " ");
    }

    private static String[] splitOnComma(String item) {
        return item.split(",");
    }

    private String getSchemaTypeFromString(String input) {
        var startIndex = input.indexOf("schema:");
        var endIndex = input.indexOf('.');
        var schemaType = input.substring(startIndex, endIndex).trim();
        return schemaType.replace("schema:", "");
    }

    private static void assertThatBasicDataIsInPlace(Publication publication,
                                                     JsonNode jsonNode,
                                                     List<String> expectedType) {
        var id = System.getenv("ID_NAMESPACE") + "/" + publication.getIdentifier().toString();
        assertThat(jsonNode.get("@id").textValue(), is(equalTo(id)));
        compareTypes(jsonNode, expectedType);
        assertThat(jsonNode.get("name").textValue(), is(not(nullValue())));
        assertThat(jsonNode.get("provider"), is(not(nullValue())));
    }

    private static void compareTypes(JsonNode jsonNode, List<String> expectedType) {
        var actual = jsonNode.get("@type");
        if (expectedType.size() == 1) {
            assertThat(actual.textValue(), is(equalTo(expectedType.get(0))));
        } else {
            assertThat(toListOfStrings(actual), containsInAnyOrder(expectedType.toArray()));
        }
    }

    private static List<String> toListOfStrings(JsonNode jsonNode) {
        return StreamSupport.stream(jsonNode.spliterator(), false)
                .map(JsonNode::textValue)
                .collect(Collectors.toList());
    }
}
