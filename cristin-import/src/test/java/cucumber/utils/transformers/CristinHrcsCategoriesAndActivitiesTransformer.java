package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActiveties;

import java.util.Map;

public class CristinHrcsCategoriesAndActivitiesTransformer {

    public static final int CURRENTLY_MAPPED_FIELDS = 2;
    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TAGS =
            String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                    CURRENTLY_MAPPED_FIELDS);
    private static final String CATEGORY = "helsekategorikode";
    private static final String ACTIVITY = "aktivitetskode";

    @DataTableType
    public CristinHrcsCategoriesAndActiveties toCristinHrcsCategoriesAndActivities(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TAGS);
        }
        String category = entry.get(CATEGORY);
        String activity = entry.get(ACTIVITY);
        return CristinHrcsCategoriesAndActiveties
                .builder()
                .withCategory(category)
                .withActivity(activity)
                .build();
    }
}
