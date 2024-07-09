package no.unit.nva.model.testing;

import no.unit.nva.model.contexttypes.PublicationContext;

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
}
