package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;

public final class ProfessionalArticleMerger extends PublicationInstanceMerger<ProfessionalArticle> {

    public ProfessionalArticleMerger(ProfessionalArticle professionalArticle) {
        super(professionalArticle);
    }

    @Override
    public ProfessionalArticle merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ProfessionalArticle professionalArticle) {
            return new ProfessionalArticle(getRange(this.publicationInstance.getPages(),
                                                    professionalArticle.getPages()),
                                           getNonNullValue(this.publicationInstance.getVolume(),
                                                           professionalArticle.getVolume()),
                                           getNonNullValue(this.publicationInstance.getIssue(),
                                                           professionalArticle.getIssue()),
                                           getNonNullValue(this.publicationInstance.getArticleNumber(),
                                                           professionalArticle.getArticleNumber()));
        } else {
            return this.publicationInstance;
        }
    }
}
