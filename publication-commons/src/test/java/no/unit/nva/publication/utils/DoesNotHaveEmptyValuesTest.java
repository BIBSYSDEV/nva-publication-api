package no.unit.nva.publication.utils;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.Identity;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.utils.DoesNotHaveEmptyValues.MissingFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class DoesNotHaveEmptyValuesTest {

    public static Stream<Named<Object>> nonEmptyValueProvider() {
        return Stream.of(
            named("Record with two strings", new SampleRecord("hi", "hi")),
            named("Record with string and record", new SampleRecord("hi", new SampleRecord("hi", "hi"))),
            named("Record with string and class", new SampleRecord("hi", new SampleClass("hi", "hi"))),
            named("Class with two strings", new SampleClass("hi", "hi")),
            named("Class with string and record", new SampleClass("hi", new SampleRecord("hi", "hi"))),
            named("Class with string and class", new SampleClass("hi", new SampleClass("hi", "hi"))),
            named("Class with string and list of class", new SampleClass("hi",
                                                                         List.of(new SampleClass("hi", "hi")))),
            named("Record with string and list of record", new SampleRecord("hi",
                                                                         List.of(new SampleRecord("hi", "hi"))))
        );
    }

    public static Stream<Named<Pair>> emptyValueProvider() {
        return Stream.of(
            named("Null top-level string", new Pair(new SampleRecord(null, "hi"), Set.of("field1"))),
            named("Empty top-level string", new Pair(new SampleRecord("", "hi"), Set.of("field1"))),
            named("Null embedded object", new Pair(new SampleRecord("hi", new SampleRecord(null, "hi")),
                                                   Set.of("field2.field1"))),
            named("Empty embedded object", new Pair(new SampleRecord("hi", new SampleRecord("", "hi")),
                                                    Set.of("field2.field1"))),
            named("Empty embedded list", new Pair(new SampleClass("hi", List.of()), Set.of("field2"))),
            named("Empty field in embedded list", new Pair(new SampleRecord("hi", List.of(new SampleRecord(null,
                                                                                                          "hi"))),
                                                          Set.of("field2.field1"))),
            named("Empty identity name", new Pair(sampleContributor(), Set.of("identity.name")))
        );
    }

    @ParameterizedTest
    @MethodSource("nonEmptyValueProvider")
    void shouldNotThrowWhenFieldsAreNotEmpty(Object testObject) {
        assertDoesNotThrow(() -> DoesNotHaveEmptyValues.checkForEmptyFields(testObject, emptySet()));
    }

    @ParameterizedTest
    @MethodSource("emptyValueProvider")
    void shouldThrowWhenFieldsAreEmpty(Pair pair) {
        var exception = assertThrows(MissingFieldException.class,
                                     () -> DoesNotHaveEmptyValues.checkForEmptyFields(pair.sampleObject(), emptySet()));
        assertThat(exception.getMessage(), containsString("Empty fields found"));
    }

    @ParameterizedTest()
    @DisplayName("Should not throw when actually empty field is excluded")
    @MethodSource("emptyValueProvider")
    void shouldNotThrowWhenEmptyFieldsAreExcluded(Pair pair) {
        assertDoesNotThrow(() ->
                               DoesNotHaveEmptyValues.checkForEmptyFields(pair.sampleObject(), pair.excludedFields()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "field2", "field2.field2"
    })
    void shouldDisregardChildrenWhenParentFieldIsExcluded(String excludedField) {
        var sample = new SampleRecord("hi", new SampleRecord("hi", new SampleRecord("", "hi")));
        assertDoesNotThrow(() -> DoesNotHaveEmptyValues.checkForEmptyFields(sample, Set.of(excludedField)));
    }

    record Pair(Object sampleObject, Set<String> excludedFields) {}

    private static Contributor sampleContributor() {
        var identity = new Identity.Builder()
                           .withNameType(NameType.PERSONAL)
                           .withId(URI.create("https://example.org/a"))
                           .withOrcId("123123123123")
                           .withAdditionalIdentifiers(List.of(new AdditionalIdentifier("a", "b")))
                           .withVerificationStatus(ContributorVerificationStatus.VERIFIED)
                           .build();
        var corporation = new Builder().withId(URI.create("https://example.org/org")).build();
        return new Contributor.Builder()
                   .withAffiliations(List.of(corporation))
                   .withRole(new RoleType(Role.ACADEMIC_COORDINATOR))
                   .withSequence(1)
                   .withIdentity(identity).build();
    }


    record SampleRecord(String field1, Object field2) {

    }

    static class SampleClass {

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