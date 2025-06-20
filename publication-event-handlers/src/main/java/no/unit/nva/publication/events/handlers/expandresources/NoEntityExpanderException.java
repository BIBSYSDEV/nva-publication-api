package no.unit.nva.publication.events.handlers.expandresources;

import no.unit.nva.publication.model.business.Entity;

public class NoEntityExpanderException extends RuntimeException{

    public NoEntityExpanderException(Class<? extends Entity> entityClass) {
        super(String.format("No entity expander found for %s", entityClass.getName()));
    }
}
