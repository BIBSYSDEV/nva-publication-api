package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;

public final class OtherStudentWorkMerger extends PublicationInstanceMerger<OtherStudentWork> {

    public OtherStudentWorkMerger(OtherStudentWork otherStudentWork) {
        super(otherStudentWork);
    }

    @Override
    public OtherStudentWork merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof OtherStudentWork newDegreeBachelor) {
            return new OtherStudentWork(getPages(this.publicationInstance.getPages(), newDegreeBachelor.getPages()),
                                        getDate(this.publicationInstance.getSubmittedDate(),
                                                newDegreeBachelor.getSubmittedDate()));
        } else {
            return this.publicationInstance;
        }
    }
}
