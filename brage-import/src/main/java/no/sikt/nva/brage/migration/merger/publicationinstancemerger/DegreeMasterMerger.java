package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;

public final class DegreeMasterMerger extends PublicationInstanceMerger<DegreeMaster> {

    public DegreeMasterMerger(DegreeMaster degreeMaster) {
        super(degreeMaster);
    }

    @Override
    public DegreeMaster merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeMaster master) {
            return new DegreeMaster(getPages(this.publicationInstance.getPages(), master.getPages()),
                                      getDate(this.publicationInstance.getSubmittedDate(),
                                              master.getSubmittedDate()));
        } else {
            return this.publicationInstance;
        }
    }
}
