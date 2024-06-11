package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static java.util.Objects.nonNull;
import java.util.LinkedHashSet;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.RelatedDocument;

public final class DegreePhdMerger extends PublicationInstanceMerger {

    private DegreePhdMerger() {
        super();
    }

    public static DegreePhd merge(DegreePhd degreePhd,
                                                             PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreePhd newDegreePhd) {
            return new DegreePhd(getPages(degreePhd.getPages(), newDegreePhd.getPages()),
                                 getDate(degreePhd.getSubmittedDate(), newDegreePhd.getSubmittedDate()),
                                 getRelated(degreePhd.getRelated(), newDegreePhd.getRelated()));
        } else {
            return degreePhd;
        }
    }

    private static Set<RelatedDocument> getRelated(Set<RelatedDocument> documents, Set<RelatedDocument> brageDocuments) {
        if (nonNull(documents) && !documents.isEmpty()) {
            var mergedDocuments = new LinkedHashSet<RelatedDocument>();
            mergedDocuments.addAll(documents);
            mergedDocuments.addAll(brageDocuments);
            return mergedDocuments;
        } else {
            return brageDocuments;
        }
    }
}
