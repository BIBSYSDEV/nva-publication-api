package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getNonNullValue;
import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getRange;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;

public final class MediaFeatureArticleMerger {

    private MediaFeatureArticleMerger() {
        super();
    }

    public static MediaFeatureArticle merge(MediaFeatureArticle mediaFeatureArticle,
                                                             PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof MediaFeatureArticle newAcademicArticle) {
            return new MediaFeatureArticle(getNonNullValue(mediaFeatureArticle.getVolume(), newAcademicArticle.getVolume()),
                                    getNonNullValue(mediaFeatureArticle.getIssue(), newAcademicArticle.getIssue()),
                                    getNonNullValue(mediaFeatureArticle.getArticleNumber(), newAcademicArticle.getArticleNumber()),
                                    getRange(mediaFeatureArticle.getPages(), newAcademicArticle.getPages()));
        } else {
            return mediaFeatureArticle;
        }
    }
}
