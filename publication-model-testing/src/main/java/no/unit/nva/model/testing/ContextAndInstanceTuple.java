package no.unit.nva.model.testing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.unit.nva.model.contexttypes.PublicationContext;

import java.net.URI;
import java.util.Set;

public class ContextAndInstanceTuple implements PublicationContext {

    private final PublicationContext context;
    private final Class<?> instanceType;

    public ContextAndInstanceTuple(PublicationContext context, Class<?> instanceType) {

        this.context = context;
        this.instanceType = instanceType;
    }

    public PublicationContext getContext() {
        return context;
    }

    public Class<?> getInstanceType() {
        return instanceType;
    }

    @JsonIgnore
    @Override
    public Set<URI> extractPublicationContextUris() {
        return context.extractPublicationContextUris();
    }
}
