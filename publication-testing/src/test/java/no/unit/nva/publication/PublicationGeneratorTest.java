package no.unit.nva.publication;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PublicationGeneratorTest {

    public static final Set<String> FIELDS_EXPECTED_TO_BE_NULL = Set.of(".doiRequest");

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    void shouldReturnPublicationWithoutEmptyFields(Class<?> publicationInstance){
        assertThat(PublicationGenerator.randomPublication(publicationInstance),
                   doesNotHaveEmptyValuesIgnoringFields(FIELDS_EXPECTED_TO_BE_NULL));
    }

}