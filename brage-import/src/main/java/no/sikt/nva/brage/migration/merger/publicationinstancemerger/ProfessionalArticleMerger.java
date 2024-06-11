package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;

public final class ProfessionalArticleMerger extends PublicationInstanceMerger<ProfessionalArticle> {

    public ProfessionalArticleMerger(ProfessionalArticle professionalArticle) {
        super(professionalArticle);
    }

    @Override
    public ProfessionalArticle merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ProfessionalArticle newProfessionalArticle) {
            return new ProfessionalArticle(getRange(this.publicationInstance.getPages(), newProfessionalArticle.getPages()),
                                           getNonNullValue(this.publicationInstance.getVolume(), newProfessionalArticle.getVolume()),
                                           getNonNullValue(this.publicationInstance.getIssue(), newProfessionalArticle.getIssue()),
                                           getNonNullValue(this.publicationInstance.getArticleNumber(), newProfessionalArticle.getArticleNumber()));
        } else {
            return this.publicationInstance;
        }
    }
}
