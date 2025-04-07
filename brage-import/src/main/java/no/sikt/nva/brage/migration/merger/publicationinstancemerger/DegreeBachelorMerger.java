package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;

public final class DegreeBachelorMerger extends PublicationInstanceMerger<DegreeBachelor> {

    public DegreeBachelorMerger(DegreeBachelor degreeBachelor) {
        super(degreeBachelor);
    }

    @Override
    public DegreeBachelor merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeBachelor bachelor) {
            return new DegreeBachelor(getPages(this.publicationInstance.getPages(), bachelor.getPages()),
                                 getDate(this.publicationInstance.getSubmittedDate(), bachelor.getSubmittedDate()));
        } else {
            return this.publicationInstance;
        }
    }
}
