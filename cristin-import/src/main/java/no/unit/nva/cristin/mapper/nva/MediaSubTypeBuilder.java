package no.unit.nva.cristin.mapper.nva;

import java.util.Map;
import no.unit.nva.cristin.mapper.CristinMediumTypeCode;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.MediaSubTypeEnum;

public class MediaSubTypeBuilder {

    private static final Map<CristinMediumTypeCode, MediaSubTypeEnum> cristinMediumTypeToNVAMediumTypeRule =
        Map.of(CristinMediumTypeCode.RADIO, MediaSubTypeEnum.RADIO,
               CristinMediumTypeCode.JOURNAL, MediaSubTypeEnum.JOURNAL,
               CristinMediumTypeCode.PROFESSIONAL_JOURNAL, MediaSubTypeEnum.JOURNAL,
               CristinMediumTypeCode.NEWSPAPER, MediaSubTypeEnum.JOURNAL,
               CristinMediumTypeCode.TV, MediaSubTypeEnum.TV,
               CristinMediumTypeCode.INTERNET, MediaSubTypeEnum.INTERNET);

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
