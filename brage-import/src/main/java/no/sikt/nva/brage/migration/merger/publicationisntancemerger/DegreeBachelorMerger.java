package no.sikt.nva.brage.migration.merger.publicationisntancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;

public final class DegreeBachelorMerger extends PublicationInstanceMerger{

    private DegreeBachelorMerger() {
        super();
    }

    public static DegreeBachelor merge(DegreeBachelor degreeBachelor,
                                                             PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeBachelor newDegreeBachelor) {
            return new DegreeBachelor(getPages(degreeBachelor.getPages(), newDegreeBachelor.getPages()),
                                 getDate(degreeBachelor.getSubmittedDate(), newDegreeBachelor.getSubmittedDate()));
        } else {
            return degreeBachelor;
        }
    }
}
