package no.unit.nva.publication.doi.update.dto;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DoiUpdateDto {

    public static ImmutableDoiUpdateDto.Builder builder() {
        return ImmutableDoiUpdateDto.builder();
    }

    public abstract Optional<URI> getDoi();

    public abstract URI getPublicationId();

    public abstract Instant getModifiedDate();
}
