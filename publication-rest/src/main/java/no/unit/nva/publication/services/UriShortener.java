package no.unit.nva.publication.services;

import java.net.URI;
import java.time.Instant;

public interface UriShortener {

    URI shorten(URI longUri, Instant expirationDate);

}
