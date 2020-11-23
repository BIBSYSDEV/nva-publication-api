package no.unit.nva.publication.doi.update.dto;

import org.immutables.value.Value;

@Value.Immutable
public abstract class DoiUpdateHolder {

    public abstract String getType();

    public abstract DoiUpdateDto getItem();

    public static ImmutableDoiUpdateHolder.Builder builder() {
        return ImmutableDoiUpdateHolder.builder();
    }
}
