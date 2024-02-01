package no.unit.nva.publication.commons.customer;

import java.net.URI;

public class CustomerNotAvailableException extends RuntimeException {

    public CustomerNotAvailableException(URI customerId) {
        super("Unable to look up customer: " + customerId);
    }

    public CustomerNotAvailableException(URI customerId, Throwable cause) {
        super("Unable to look up customer: " + customerId, cause);
    }
}
