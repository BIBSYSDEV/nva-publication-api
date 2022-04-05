package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import java.util.Map;
import no.unit.nva.cristin.mapper.CristinPresentationalWork;

public class CristinPresentationalWorkTransformer {

    public static final int CURRENTLY_MAPPED_FIELDS = 2;
    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS =
            String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                    CURRENTLY_MAPPED_FIELDS);
    private static final String ID = "Identifier";
    private static final String TYPE = "Type";

    @DataTableType
    public CristinPresentationalWork toCristinPresentationalWork(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS);
        }

        String type = entry.get(TYPE);
        int id = Integer.parseInt(entry.get(ID));

        return CristinPresentationalWork.builder()
                .withIdentifier(id)
                .withPresentationType(type)
                .build();
    }
}
