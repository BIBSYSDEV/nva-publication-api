package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;

public final class OtherStudentWorkMerger extends PublicationInstanceMerger<OtherStudentWork> {

    public OtherStudentWorkMerger(OtherStudentWork otherStudentWork) {
        super(otherStudentWork);
    }

    @Override
    public OtherStudentWork merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof OtherStudentWork studentWork) {
            return new OtherStudentWork(getPages(this.publicationInstance.getPages(), studentWork.getPages()),
                                        getDate(this.publicationInstance.getSubmittedDate(),
                                                studentWork.getSubmittedDate()));
        } else {
            return this.publicationInstance;
        }
    }
}
