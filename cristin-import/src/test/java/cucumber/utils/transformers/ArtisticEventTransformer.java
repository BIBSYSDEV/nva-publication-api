package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import no.unit.nva.cristin.mapper.artisticproduction.ArtisticEvent;

import java.util.Map;

public class ArtisticEventTransformer {

    private static final String START = "start";
    private static final String TITLE = "title";
    private static final String PLACE = "place";
    private static final int CURRENTLY_MAPPED_FIELDS = 3;
    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
            CURRENTLY_MAPPED_FIELDS);

    @DataTableType
    public static ArtisticEvent toArtisticEevent(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS);
        }
        var start = entry.get(START);
        var title = entry.get(TITLE);
        var place = entry.get(PLACE);
        return ArtisticEvent
            .builder()
            .withDateFrom(start)
            .withTitle(title)
            .withPlace(place)
            .build();
    }
}
