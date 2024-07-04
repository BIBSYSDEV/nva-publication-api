package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;

public final class MediaReaderOpinionMerger extends PublicationInstanceMerger<MediaReaderOpinion> {

    public MediaReaderOpinionMerger(MediaReaderOpinion mediaReaderOpinion) {
        super(mediaReaderOpinion);
    }

    @Override
    public MediaReaderOpinion merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof MediaReaderOpinion newMediaReaderOpinion) {
            return new MediaReaderOpinion(getNonNullValue(this.publicationInstance.getVolume(),
                                                          newMediaReaderOpinion.getVolume()),
                                          getNonNullValue(this.publicationInstance.getIssue(),
                                                          newMediaReaderOpinion.getIssue()),
                                          getNonNullValue(this.publicationInstance.getArticleNumber(),
                                                          newMediaReaderOpinion.getArticleNumber()),
                                          getRange(this.publicationInstance.getPages(),
                                                   newMediaReaderOpinion.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
