package no.sikt.nva.brage.migration.merger.publicationisntancemerger;

import static no.sikt.nva.brage.migration.merger.publicationisntancemerger.PublicationInstanceMerger.getDate;
import static no.sikt.nva.brage.migration.merger.publicationisntancemerger.PublicationInstanceMerger.getPages;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;

public class OtherStudentWorkMerger {

    private OtherStudentWorkMerger() {
    }

    public static OtherStudentWork merge(OtherStudentWork otherStudentWork,
                                         PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof OtherStudentWork newDegreeBachelor) {
            return new OtherStudentWork(getPages(otherStudentWork.getPages(), newDegreeBachelor.getPages()),
                                        getDate(otherStudentWork.getSubmittedDate(),
                                                newDegreeBachelor.getSubmittedDate()));
        } else {
            return otherStudentWork;
        }
    }
}
