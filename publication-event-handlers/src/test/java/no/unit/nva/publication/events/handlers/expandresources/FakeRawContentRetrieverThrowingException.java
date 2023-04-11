package no.unit.nva.publication.events.handlers.expandresources;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.publication.external.services.RawContentRetriever;

public class FakeRawContentRetrieverThrowingException implements RawContentRetriever {

    @Override
    public Optional<String> getRawContent(URI uri, String mediaType) {
        throw new RuntimeException("I don't work");
    }
}
