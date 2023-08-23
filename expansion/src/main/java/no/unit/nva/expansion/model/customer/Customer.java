package no.unit.nva.expansion.model.customer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class Customer {

    public static final String ID = "id";
    @JsonProperty(ID)
    private final URI id;

    @JsonCreator
    public Customer(@JsonProperty(ID) URI id) {
        this.id = id;
    }

    public URI getId() {
        return id;
    }
}
