package no.unit.nva.model.testing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.unit.nva.model.contexttypes.PublicationContext;

import java.net.URI;
import java.util.Set;

public record ContextAndInstanceTuple(PublicationContext context, Class<?> instanceType) implements PublicationContext {

    @JsonIgnore
    @Override
    public Set<URI> extractPublicationContextUris() {
        return context.extractPublicationContextUris();
    }
}
