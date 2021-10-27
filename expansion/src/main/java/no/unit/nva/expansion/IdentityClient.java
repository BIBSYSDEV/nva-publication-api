package no.unit.nva.expansion;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public interface IdentityClient {

    Optional<URI> getCustomerId(String username);
    Optional<URI> getCristinId(URI customerId);

}
