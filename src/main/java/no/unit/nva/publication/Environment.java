package no.unit.nva.publication;

import java.util.Optional;

import static java.util.Objects.isNull;

public class Environment {

    /**
     * Get environment variable.
     *
     * @param name  name of environment variable
     * @return  optional with value of environment variable
     */
    public Optional<String> get(String name) {
        String environmentVariable = System.getenv(name);

        if (isNull(environmentVariable) || environmentVariable.isEmpty()) {
            return Optional.empty();
        }

        return  Optional.of(environmentVariable);
    }
}
