package no.unit.nva.cristin.mapper.nva;

import java.util.Map;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.MediaSubTypeEnum;

public class MediaSubTypeBuilder {

    private static final Map<String, MediaSubTypeEnum> cristinMediumTypeToNVAMediumTypeRule =
        Map.of("RADIO", MediaSubTypeEnum.RADIO,
               "TIDSSKRIFT", MediaSubTypeEnum.JOURNAL,
               "FAGBLAD", MediaSubTypeEnum.JOURNAL,
               "AVIS", MediaSubTypeEnum.JOURNAL,
               "TV", MediaSubTypeEnum.TV,
               "INTERNETT", MediaSubTypeEnum.INTERNET);

    private final CristinObject cristinObject;

    public MediaSubTypeBuilder(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public MediaSubType build() {
        var cristinMediumTypeCode = cristinObject.getMediaContribution().getCristinMediumType().getMediumTypeCode();
        var mediaSubTypeEnum = cristinMediumTypeToNVAMediumTypeRule.getOrDefault(cristinMediumTypeCode,
                                                                                 MediaSubTypeEnum.OTHER);
        return MediaSubType.create(mediaSubTypeEnum);
    }
}
