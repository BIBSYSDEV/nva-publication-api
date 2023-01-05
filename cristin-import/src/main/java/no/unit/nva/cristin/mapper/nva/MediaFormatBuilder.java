package no.unit.nva.cristin.mapper.nva;

import java.util.Map;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.media.MediaFormat;

public class MediaFormatBuilder {

    private final static Map<String, MediaFormat> cristinMediumTypeToNVaFormat =
        Map.of("TIDSSKRIFT", MediaFormat.TEXT,
               "FAGBLAD", MediaFormat.TEXT,
               "AVIS", MediaFormat.TEXT,
               "TV", MediaFormat.VIDEO,
               "RADIO", MediaFormat.SOUND);
    private final CristinObject cristinObject;

    public MediaFormatBuilder(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public MediaFormat build() {
        var mediumTypeCode = cristinObject.getMediaContribution().getCristinMediumType().getMediumTypeCode();
        return cristinMediumTypeToNVaFormat.getOrDefault(mediumTypeCode, null);
    }
}
