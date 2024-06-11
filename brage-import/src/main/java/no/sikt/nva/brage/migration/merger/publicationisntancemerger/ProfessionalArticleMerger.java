package no.sikt.nva.brage.migration.merger.publicationisntancemerger;

import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getNonNullValue;
import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getRange;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;

public final class ProfessionalArticleMerger {

    private ProfessionalArticleMerger() {
        super();
    }

    public static ProfessionalArticle merge(ProfessionalArticle professionalArticle,
                                            PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ProfessionalArticle newProfessionalArticle) {
            return new ProfessionalArticle(getRange(professionalArticle.getPages(), newProfessionalArticle.getPages()),
                                           getNonNullValue(professionalArticle.getVolume(), newProfessionalArticle.getVolume()),
                                           getNonNullValue(professionalArticle.getIssue(), newProfessionalArticle.getIssue()),
                                           getNonNullValue(professionalArticle.getArticleNumber(), newProfessionalArticle.getArticleNumber()));
        } else {
            return professionalArticle;
        }
    }
}
