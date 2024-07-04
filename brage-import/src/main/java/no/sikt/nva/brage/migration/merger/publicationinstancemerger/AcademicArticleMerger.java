package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;

public final class AcademicArticleMerger extends PublicationInstanceMerger<AcademicArticle> {

    public AcademicArticleMerger(AcademicArticle academicArticle) {
        super(academicArticle);
    }

    @Override
    public AcademicArticle merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof AcademicArticle newAcademicArticle) {
            return new AcademicArticle(getRange(this.publicationInstance.getPages(),
                                                newAcademicArticle.getPages()),
                                                getNonNullValue(this.publicationInstance.getVolume(),
                                                                newAcademicArticle.getVolume()),
                                                getNonNullValue(this.publicationInstance.getIssue(),
                                                                newAcademicArticle.getIssue()),
                                                getNonNullValue(this.publicationInstance.getArticleNumber(),
                                                                newAcademicArticle.getArticleNumber()));
        } else {
            return this.publicationInstance;
        }
    }
}
