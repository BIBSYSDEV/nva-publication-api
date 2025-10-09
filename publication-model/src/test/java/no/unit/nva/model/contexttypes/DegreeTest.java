package no.unit.nva.model.contexttypes;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.UnconfirmedCourse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DegreeTest {

    @ParameterizedTest(name = "should consume course from JSON field: {0}")
    @MethodSource("courseFieldNames")
    void shouldConsumeCourseFromJson(String fieldName) throws JsonProcessingException {
        var value = randomString();
        var json = """
            {
                "type": "Degree",
                "%s": {
                    "type": "UnconfirmedCourse",
                    "code": "%s"
                }
            }
            """.formatted(fieldName, value);

        var book = JsonUtils.dtoObjectMapper.readValue(json, Book.class);
        var degree = (Degree) book;
        var course = (UnconfirmedCourse) degree.getCourse();
        assertEquals(value, course.code());
    }

    private static Stream<Arguments> courseFieldNames() {
        return Stream.of(Arguments.of("course"), Arguments.of("courseCode"));
    }
}
