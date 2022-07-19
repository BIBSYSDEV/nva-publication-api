package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import java.util.Map;
import no.unit.nva.cristin.mapper.CristinSubjectField;

public class CristinSubjectFieldTransformer {
    
    public static final int CURRENTLY_MAPPED_FIELDS = 1;
    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_SUBJECT_FIELD =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
            CURRENTLY_MAPPED_FIELDS);
    private static final String FIELD_CODE = "FieldCode";
    
    @DataTableType
    public CristinSubjectField toCristinSubjectField(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_SUBJECT_FIELD);
        }
        int fieldCode = Integer.parseInt(entry.get(FIELD_CODE));
        return CristinSubjectField.builder()
            .withSubjectFieldCode(fieldCode)
            .build();
    }
}
