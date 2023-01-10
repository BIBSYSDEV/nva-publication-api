package no.unit.nva.cristin.mapper.nva;

import java.util.Map;
import no.unit.nva.cristin.mapper.CristinMediumTypeCode;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.media.MediaFormat;

public class MediaFormatBuilder {

    private final static Map<CristinMediumTypeCode, MediaFormat> cristinMediumTypeToNVaFormat =
        Map.of(CristinMediumTypeCode.JOURNAL, MediaFormat.TEXT,
               CristinMediumTypeCode.PROFESSIONAL_JOURNAL, MediaFormat.TEXT,
               CristinMediumTypeCode.NEWSPAPER, MediaFormat.TEXT,
               CristinMediumTypeCode.TV, MediaFormat.VIDEO,
               CristinMediumTypeCode.RADIO, MediaFormat.SOUND);
    private final CristinObject cristinObject;

    public MediaFormatBuilder(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public MediaFormat build() {
        var mediumTypeCode = cristinObject.getMediaContribution().getCristinMediumType().getMediumTypeCode();
        return cristinMediumTypeToNVaFormat.getOrDefault(mediumTypeCode, null);
    }
}
