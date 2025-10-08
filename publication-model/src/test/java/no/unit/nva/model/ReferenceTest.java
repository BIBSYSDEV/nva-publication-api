package no.unit.nva.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReferenceTest {

    public static Stream<Arguments> contextTypePairs() {
        var contextTypes = List.of(
            "Book",
            "Report",
            "Degree",
            "Anthology",
            "UnconfirmedJournal",
            "Event",
            "Artistic",
            "ResearchData",
            "GeographicalContent",
            "ExhibitionContent"
        );
        return IntStream.range(0, contextTypes.size())
                   .boxed()
                   .flatMap(i -> IntStream.range(i + 1, contextTypes.size())
                                     .mapToObj(j -> Arguments.of(contextTypes.get(i), contextTypes.get(j)))
                   );
    }

    @ParameterizedTest(name = "References with instance type {0} and {1} are not equal")
    @MethodSource("instanceTypePairs")
    void referenceWithTwoDifferentInstanceTypesShouldNotBeEqual(String instanceTypeOne, String instanceTypeTwo) throws JsonProcessingException {
        var oldJson = """
            {
              "type": "Reference",
              "publicationInstance": {
                "type": "%s"
              }
            }
            """.formatted(instanceTypeOne);
        var json = """
            {
              "type": "Reference",
              "publicationInstance": {
                "type": "%s"
              }
            }
            """.formatted(instanceTypeTwo);

        var old = JsonUtils.dtoObjectMapper.readValue(oldJson, Reference.class);
        var updated = JsonUtils.dtoObjectMapper.readValue(json, Reference.class);

        assertNotEquals(old, updated);
    }

    @ParameterizedTest(name = "References with context type {0} and {1} are not equal")
    @MethodSource("contextTypePairs")
    void referenceWithTwoDifferentContextTypesShouldNotBeEqual(String eventOne, String eventTwo) throws JsonProcessingException {
        var oldJson = """
            {
              "type": "Reference",
              "publicationContext": {
                "type": "%s"
              }
            }
            """.formatted(eventOne);
        var json = """
            {
              "type": "Reference",
              "publicationContext": {
                "type": "%s"
              }
            }
            """.formatted(eventTwo);

        var old = JsonUtils.dtoObjectMapper.readValue(oldJson, Reference.class);
        var updated = JsonUtils.dtoObjectMapper.readValue(json, Reference.class);

        assertNotEquals(old, updated);
    }

    @Test
    void referenceMissingContextTypeAndInstanceTypeShouldNotFailOnEquals() {
        var reference = new Reference();
        assertDoesNotThrow(() -> reference.equals(new Reference()));
    }

    private static Stream<Arguments> instanceTypePairs() {
        var types = List.of("ConferenceLecture", "OtherPresentation", "ConferencePoster", "Lecture");

        return IntStream.range(0, types.size())
                   .boxed()
                   .flatMap(i -> IntStream.range(i + 1, types.size())
                                     .mapToObj(j -> Arguments.of(types.get(i), types.get(j))));
    }
}