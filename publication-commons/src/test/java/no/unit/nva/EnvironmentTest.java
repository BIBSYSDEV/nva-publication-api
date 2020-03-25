package no.unit.nva;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class EnvironmentTest {

    public static final String TEST_ENV = "TEST";
    public static final String PATH_ENV = "PATH"; // known to always exist

    @Test
    public void testEnv() {
        Environment environment = new Environment();
        Optional<String> path = environment.get(PATH_ENV);
        assertTrue(path.isPresent());
    }

    @Test
    public void testNoEnv() {
        Environment environment = new Environment();
        Optional<String> test = environment.get(TEST_ENV);
        assertTrue(test.isEmpty());
    }

}
