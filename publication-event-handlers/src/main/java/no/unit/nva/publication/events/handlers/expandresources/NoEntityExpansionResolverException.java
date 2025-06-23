package no.unit.nva.publication.events.handlers.expandresources;

import no.unit.nva.publication.model.business.Entity;

public class NoEntityExpansionResolverException extends RuntimeException{

    public NoEntityExpansionResolverException(Class<? extends Entity> entityClass) {
        super(String.format("No entity expansion strategy found for %s", entityClass.getName()));
    }
}
