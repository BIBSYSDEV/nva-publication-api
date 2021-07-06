package cucumber;

import io.cucumber.java.DataTableType;
import java.util.Map;
import no.unit.nva.cristin.mapper.CristinTitle;

public class CristinTitleTransformer {

    public static final int CURRENTLY_MAPPED_FIELDS = 2;
    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTINT_TITLE =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                      CURRENTLY_MAPPED_FIELDS);
    private static final String TITLE_TEXT = "Title Text";
    private static final String STATUS_ORIGINAL = "Status Original";

    @DataTableType
    public CristinTitle toCristinTitle(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTINT_TITLE);
        }
        String titleText = entry.get(TITLE_TEXT);
        String statusOriginal = entry.get(STATUS_ORIGINAL);
        return CristinTitle.builder().withTitle(titleText).withStatusOriginal(statusOriginal).build();
    }
}
