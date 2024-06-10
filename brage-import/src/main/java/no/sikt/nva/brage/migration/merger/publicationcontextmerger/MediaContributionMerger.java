package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import static java.util.Objects.nonNull;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.media.MediaFormat;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.SeriesEpisode;

public final class MediaContributionMerger extends PublicationContextMerger {

    private MediaContributionMerger() {
    }

    public static MediaContribution merge(MediaContribution mediaContribution, PublicationContext publicationContext) {
        if (publicationContext instanceof MediaContribution newMediaContribution) {
            return new MediaContribution.Builder()
                       .withMedium(getMedium(mediaContribution, newMediaContribution))
                       .withFormat(getFormat(mediaContribution, newMediaContribution))
                       .withPartOf(getPartOf(mediaContribution, newMediaContribution))
                       .withDisseminationChannel(getNonNullValue(
                           mediaContribution.getDisseminationChannel(),
                           newMediaContribution.getDisseminationChannel()))
                       .build();
        } else {
            return mediaContribution;
        }
    }

    private static SeriesEpisode getPartOf(MediaContribution oldMediaContribution,
                                           MediaContribution newMediaContribution) {
        return nonNull(oldMediaContribution.getPartOf())
                   ? oldMediaContribution.getPartOf()
                   : newMediaContribution.getPartOf();
    }

    private static MediaFormat getFormat(MediaContribution oldMediaContribution,
                                         MediaContribution newMediaContribution) {
        return nonNull(oldMediaContribution.getFormat())
                   ? oldMediaContribution.getFormat()
                   : newMediaContribution.getFormat();
    }

    private static MediaSubType getMedium(MediaContribution oldMediaContribution,
                                          MediaContribution newMediaContribution) {
        return nonNull(oldMediaContribution.getMedium())
                   ? oldMediaContribution.getMedium()
                   : newMediaContribution.getMedium();
    }
}
