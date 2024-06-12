package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;

public class NoMerger<T extends PublicationInstance<?>> extends PublicationInstanceMerger<T> {
    public NoMerger(T instance) {
        super(instance);
    }

    @Override
    public T merge(PublicationInstance<?> newInstance) {
        return this.publicationInstance;
    }
}