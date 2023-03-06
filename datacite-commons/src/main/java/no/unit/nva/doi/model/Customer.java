package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class Customer {

    private final URI id;
    private final String name;

    @JsonCreator
    public Customer(@JsonProperty("id")URI id, @JsonProperty("name")String name) {
        this.id = id;
        this.name = name;

    }
    public String getName() {
        return name;
    }

    public URI getId() {
        return id;
    }
}
