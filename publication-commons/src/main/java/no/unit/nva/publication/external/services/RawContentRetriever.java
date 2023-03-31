package no.unit.nva.publication.external.services;

import java.net.URI;
import java.util.Optional;

public interface RawContentRetriever {

    Optional<String> getRawContent(URI uri, String mediaType);

    <T> Optional<T> getDto(URI uri , Class<T> valueType);
}
