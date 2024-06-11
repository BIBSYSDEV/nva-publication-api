package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;

public final class DegreeMasterMerger extends PublicationInstanceMerger<DegreeMaster> {

    public DegreeMasterMerger(DegreeMaster degreeMaster) {
        super(degreeMaster);
    }

    @Override
    public DegreeMaster merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeMaster newDegreeBachelor) {
            return new DegreeMaster(getPages(this.publicationInstance.getPages(), newDegreeBachelor.getPages()),
                                      getDate(this.publicationInstance.getSubmittedDate(), newDegreeBachelor.getSubmittedDate()));
        } else {
            return this.publicationInstance;
        }
    }
}
