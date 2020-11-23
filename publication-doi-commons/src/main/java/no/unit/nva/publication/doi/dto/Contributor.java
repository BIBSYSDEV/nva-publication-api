package no.unit.nva.publication.doi.dto;

import java.net.URI;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable

public abstract class Contributor {

    public static ImmutableContributor.Builder builder() {
        return ImmutableContributor.builder();
    }

    public abstract Optional<URI> getId();

    public abstract String getArpId();

    public abstract String getName();
}
