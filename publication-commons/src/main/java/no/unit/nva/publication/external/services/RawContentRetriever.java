package no.unit.nva.publication.external.services;

import java.net.URI;
import java.util.Optional;

public interface RawContentRetriever {

    Optional<String> getRawContent(URI uri, String mediaType);
}
