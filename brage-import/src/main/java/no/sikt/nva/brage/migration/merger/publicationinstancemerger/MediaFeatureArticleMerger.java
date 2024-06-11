package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;

public final class MediaFeatureArticleMerger extends PublicationInstanceMerger<MediaFeatureArticle> {

    public MediaFeatureArticleMerger(MediaFeatureArticle mediaFeatureArticle) {
        super(mediaFeatureArticle);
    }

    @Override
    public MediaFeatureArticle merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof MediaFeatureArticle newAcademicArticle) {
            return new MediaFeatureArticle(getNonNullValue(this.publicationInstance.getVolume(), newAcademicArticle.getVolume()),
                                    getNonNullValue(this.publicationInstance.getIssue(), newAcademicArticle.getIssue()),
                                    getNonNullValue(this.publicationInstance.getArticleNumber(), newAcademicArticle.getArticleNumber()),
                                    getRange(this.publicationInstance.getPages(), newAcademicArticle.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
