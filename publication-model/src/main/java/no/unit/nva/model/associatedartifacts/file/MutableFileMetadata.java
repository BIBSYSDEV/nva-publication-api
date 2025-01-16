package no.unit.nva.model.associatedartifacts.file;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

public interface MutableFileMetadata {

    URI getLicense();

    PublisherVersion getPublisherVersion();

    Optional<Instant> getEmbargoDate();

    String getLegalNote();
}
