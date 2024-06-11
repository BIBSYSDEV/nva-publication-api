package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getNonNullValue;
import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getRange;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;

public final class AcademicArticleMerger {

    private AcademicArticleMerger() {
        super();
    }

    public static AcademicArticle merge(AcademicArticle academicArticle,
                                                             PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof AcademicArticle newAcademicArticle) {
            return new AcademicArticle(getRange(academicArticle.getPages(), newAcademicArticle.getPages()),
                                                getNonNullValue(academicArticle.getVolume(), newAcademicArticle.getVolume()),
                                                getNonNullValue(academicArticle.getIssue(), newAcademicArticle.getIssue()),
                                                getNonNullValue(academicArticle.getArticleNumber(), newAcademicArticle.getArticleNumber()));
        } else {
            return academicArticle;
        }
    }
}
