package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static java.util.Objects.nonNull;
import java.util.LinkedHashSet;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.RelatedDocument;

public final class DegreePhdMerger extends PublicationInstanceMerger<DegreePhd> {

    public DegreePhdMerger(DegreePhd degreePhd) {
        super(degreePhd);
    }

    @Override
    public DegreePhd merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreePhd newDegreePhd) {
            return new DegreePhd(getPages(this.publicationInstance.getPages(), newDegreePhd.getPages()),
                                 getDate(this.publicationInstance.getSubmittedDate(), newDegreePhd.getSubmittedDate()),
                                 getRelated(this.publicationInstance.getRelated(), newDegreePhd.getRelated()));
        } else {
            return this.publicationInstance;
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
