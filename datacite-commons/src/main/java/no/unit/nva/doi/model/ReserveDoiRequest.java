package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class ReserveDoiRequest {

    private final URI customer;

    public URI getCustomer() {
        return customer;
    }

    @JsonCreator
    public ReserveDoiRequest(@JsonProperty("customer") URI customer) {
        this.customer = customer;
    }
}
