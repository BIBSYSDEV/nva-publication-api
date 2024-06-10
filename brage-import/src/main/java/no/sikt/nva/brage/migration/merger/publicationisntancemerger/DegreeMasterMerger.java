package no.sikt.nva.brage.migration.merger.publicationisntancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;

public final class DegreeMasterMerger extends PublicationInstanceMerger{

    private DegreeMasterMerger() {
        super();
    }

    public static DegreeMaster merge(DegreeMaster degreeMaster,
                                       PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeMaster newDegreeBachelor) {
            return new DegreeMaster(getPages(degreeMaster.getPages(), newDegreeBachelor.getPages()),
                                      getDate(degreeMaster.getSubmittedDate(), newDegreeBachelor.getSubmittedDate()));
        } else {
            return degreeMaster;
        }
    }
}
