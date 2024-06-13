package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;

public final class DegreeBachelorMerger extends PublicationInstanceMerger<DegreeBachelor> {

    public DegreeBachelorMerger(DegreeBachelor degreeBachelor) {
        super(degreeBachelor);
    }

    public DegreeBachelor merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeBachelor newDegreeBachelor) {
            return new DegreeBachelor(getPages(this.publicationInstance.getPages(), newDegreeBachelor.getPages()),
                                 getDate(this.publicationInstance.getSubmittedDate(), newDegreeBachelor.getSubmittedDate()));
        } else {
            return this.publicationInstance;
        }
    }
}
