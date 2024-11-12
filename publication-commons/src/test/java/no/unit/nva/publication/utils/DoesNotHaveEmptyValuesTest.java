package no.unit.nva.publication.utils;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DoesNotHaveEmptyValuesTest {

    public static Stream<Named<SampleObject>> nonEmptyValueProvider() {
        return Stream.of(
            named("Record with two strings", new SampleRecord("hi", "hi")),
            named("Record with string and record", new SampleRecord("hi", new SampleRecord("hi", "hi"))),
            named("Record with string and class", new SampleRecord("hi", new SampleClass("hi", "hi"))),
            named("Class with two strings", new SampleClass("hi", "hi")),
            named("Class with string and record", new SampleClass("hi", new SampleRecord("hi", "hi"))),
            named("Class with string and class", new SampleClass("hi", new SampleClass("hi", "hi")))
        );
    }

    public static Stream<Named<SampleRecord>> emptyValueProvider() {
        return Stream.of(
            named("Null top-level string", new SampleRecord(null, "hi")),
            named("Empty top-level string", new SampleRecord("", "hi")),
            named("Null embedded object", new SampleRecord("hi", new SampleRecord(null, "hi"))),
            named("Empty embedded object", new SampleRecord("hi", new SampleRecord("", "hi")))
        );
    }

    @ParameterizedTest
    @MethodSource("nonEmptyValueProvider")
    void shouldNotThrowWhenFieldsAreNotEmpty(SampleObject testObject) {
        assertDoesNotThrow(() -> DoesNotHaveEmptyValues.checkForEmptyFields(testObject, emptySet()));
    }

    @ParameterizedTest
    @MethodSource("emptyValueProvider")
    void shouldThrowWhenFieldsAreEmpty(SampleRecord testObject) {
        var exception = assertThrows(RuntimeException.class, () -> DoesNotHaveEmptyValues.checkForEmptyFields(testObject,
                                                                                                emptySet()));
        assertThat(exception.getMessage(), containsString("field1"));
    }

    @ParameterizedTest
    @MethodSource("emptyValueProvider")
    void shouldNotThrowWhenEmptyFieldsAreExcluded(SampleRecord testObject) {
        assertDoesNotThrow(() -> DoesNotHaveEmptyValues.checkForEmptyFields(testObject, Set.of("field1")));
    }

    interface SampleObject {
        // Marker pattern
    }

    record SampleRecord(String field1, Object field2) implements SampleObject {

    }

    static class SampleClass implements SampleObject {

        private final String field1;
        private final Object field2;

        public SampleClass(String field1, Object field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public String getField1() {
            return field1;
        }

        public Object getField2() {
            return field2;
        }
    }
}