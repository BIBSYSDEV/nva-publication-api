package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.performingarts.realization.PerformingArtsVenue;
import no.unit.nva.model.time.Period;
import no.unit.nva.model.time.Time;

import java.util.Map;

public class NvaPerformingArtsVenue {

    private static final int CURRENTLY_MAPPED_FIELDS = 4;
    private static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
            CURRENTLY_MAPPED_FIELDS);

    @DataTableType
    public static PerformingArtsVenue toPerformingArtsVenue(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS);
        }
        var to = entry.get("to");
        var from = entry.get("from");
        var place = entry.get("place");
        var sequence = Integer.parseInt(entry.get("sequence"));

        return new PerformingArtsVenue(
            new UnconfirmedPlace(place, null),
            new Period(Time.convertToInstant(from), Time.convertToInstant(to)),
            sequence
        );
    }
}
