package no.unit.publication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class EnvironmentTest {

    public static final String TEST_ENV = "TEST";
    public static final String PATH_ENV = "PATH"; // known to always exist

    @Test
    @DisplayName("reading Existing Env Variable Returns Value")
    public void readingExistingEnvVariableReturnsValue() {
        Environment environment = new Environment();
        Optional<String> path = environment.get(PATH_ENV);
        assertTrue(path.isPresent());
    }

    @Test
    @DisplayName("reading Missing Env Variable Returns Empty Value")
    public void readingMissingEnvVariableReturnsEmptyValue() {
        Environment environment = new Environment();
        Optional<String> test = environment.get(TEST_ENV);
        assertTrue(test.isEmpty());
    }

}
