package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;

public final class MediaReaderOpinionMerger extends PublicationInstanceMerger {

    private MediaReaderOpinionMerger() {
        super();
    }

    public static MediaReaderOpinion merge(MediaReaderOpinion mediaReaderOpinion, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof MediaReaderOpinion newMediaReaderOpinion) {
            return new MediaReaderOpinion(getNonNullValue(mediaReaderOpinion.getVolume(), newMediaReaderOpinion.getVolume()),
                                          getNonNullValue(mediaReaderOpinion.getIssue(), newMediaReaderOpinion.getIssue()),
                                          getNonNullValue(mediaReaderOpinion.getArticleNumber(), newMediaReaderOpinion.getArticleNumber()),
                                          getRange(mediaReaderOpinion.getPages(), newMediaReaderOpinion.getPages()));
        } else {
            return mediaReaderOpinion;
        }
    }
}
