package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Customer {

    private final String name;
    private final URI id;

    @JsonCreator
    public Customer(@JsonProperty("name") String name,
                    @JsonProperty("id") URI id) {
        this.name = name;
        this.id = id;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getId());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer customer = (Customer) o;
        return Objects.equals(getName(), customer.getName()) && Objects.equals(getId(),
                                                                               customer.getId());
    }

    public String getName() {
        return name;
    }

    public URI getId() {
        return id;
    }
}
