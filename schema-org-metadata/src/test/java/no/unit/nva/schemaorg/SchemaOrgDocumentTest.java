package no.unit.nva.schemaorg;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.chapter.PopularScienceChapter;
import no.unit.nva.model.instancetypes.journal.PopularScienceArticle;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.schemaorg.document.FramedSchemaOrgDocumentBuilder;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.linesfromResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;


class SchemaOrgDocumentTest {

    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
    public static final String PREFIXED_TURTLE_TOKEN_TEMPLATE = ":%s ";
    public static final String COMMA = ",";
    public static final String JSON_ARRAY_DELIMITER = COMMA;
    public static final String ID_NAMESPACE = "ID_NAMESPACE";
    public static final String SCHEMA_ORG_NS_PREFIX = "schema:";
    public static final String EMPTY_STRING = "";
    public static final char TURTLE_STATEMENT_END = '.';
    public static final Path RDF_SUBTYPE_MAPPINGS = Path.of("subtype_mappings.ttl");

    public static Stream<Class<?>> instanceTypeProvider() {
        return TypeProvider.listSubTypes(PublicationInstance.class);
    }

    @ParameterizedTest(name = "Should generate schema.org for type: {0}")
    @MethodSource("instanceTypeProvider")
    void shouldReturnSchemaOrgDocumentWithTypeMappingWhenInputIsPublication(
            Class<? extends PublicationInstance<?>> instance) {
        var publication = randomPublication(instance);
        var actual = SchemaOrgDocument.fromPublication(publication);
        var expected = generateExpected(publication, instance);
        assertThat(actual, sameJSONAs(expected).allowingAnyArrayOrdering());
    }

    private String generateExpected(Publication publication,
                                                     Class<? extends PublicationInstance<?>> instance) {
        return attempt(() -> MAPPER.writeValueAsString(FramedSchemaOrgDocumentBuilder.newInstance()
                .withCreator(publication.getEntityDescription().getContributors())
                .withName(publication.getEntityDescription().getMainTitle())
                .withId(constructExpectedId(publication))
                .withProvider(URI.create("https://sikt.no"), "Sikt")
                .withContext(URI.create("https://schema.org/"))
                .withType(calculateExpectedTypes(instance))
                .build())).orElseThrow();
    }

    private List<String> calculateExpectedTypes(Class<? extends PublicationInstance<?>> instance) {
        var name = instance.getSimpleName();
        return linesfromResource(RDF_SUBTYPE_MAPPINGS)
                .stream()
                .filter(item -> matchRdfPrefixedTerm(name, item))
                .map(this::getSchemaTypeFromTurtleString)
                .map(SchemaOrgDocumentTest::splitJsonArray)
                .flatMap(Arrays::stream)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static boolean matchRdfPrefixedTerm(String name, String item) {
        return item.contains(String.format(PREFIXED_TURTLE_TOKEN_TEMPLATE, name));
    }

    private static String[] splitJsonArray(String item) {
        return item.split(JSON_ARRAY_DELIMITER);
    }

    private String getSchemaTypeFromTurtleString(String input) {
        var startIndex = input.indexOf(SCHEMA_ORG_NS_PREFIX);
        var endIndex = input.indexOf(TURTLE_STATEMENT_END);
        var schemaType = input.substring(startIndex, endIndex).trim();
        return schemaType.replace(SCHEMA_ORG_NS_PREFIX, EMPTY_STRING);
    }

    private static URI constructExpectedId(Publication publication) {
        return UriWrapper.fromUri(System.getenv(ID_NAMESPACE))
                .addChild(publication.getIdentifier().toString())
                .getUri();
    }
}
