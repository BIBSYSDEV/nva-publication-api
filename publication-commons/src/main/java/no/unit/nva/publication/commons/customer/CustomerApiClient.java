package no.unit.nva.publication.commons.customer;

import java.net.URI;

public interface CustomerApiClient {
    Customer fetch(URI customerId);
}
