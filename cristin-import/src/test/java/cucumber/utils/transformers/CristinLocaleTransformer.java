package cucumber.utils.transformers;

import static no.unit.nva.cristin.mapper.CristinLocale.DEPARTMENT_IDENTIFIER_FIELD;
import static no.unit.nva.cristin.mapper.CristinLocale.GROUP_IDENTIFIER_FIELD;
import static no.unit.nva.cristin.mapper.CristinLocale.INSTITUTION_IDENTIFIER_FIELD;
import static no.unit.nva.cristin.mapper.CristinLocale.OWNER_CODE_FIELD;
import static no.unit.nva.cristin.mapper.CristinLocale.SUB_DEPARTMENT_IDENTIFIER_FIELD;
import io.cucumber.java.DataTableType;
import java.util.Map;
import no.unit.nva.cristin.mapper.CristinLocale;

public class CristinLocaleTransformer {

    private static final int CURRENTLY_MAPPED_FIELDS = 5;

    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TITLE =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                      CURRENTLY_MAPPED_FIELDS);

    @DataTableType
    public CristinLocale toCristinLocale(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TITLE);
        }
        return CristinLocale.builder()
                   .withOwnerCode(entry.get(OWNER_CODE_FIELD))
                   .withInstitutionIdentifier(entry.get(INSTITUTION_IDENTIFIER_FIELD))
                   .withDepartmentIdentifier(entry.get(DEPARTMENT_IDENTIFIER_FIELD))
                   .withSubDepartmentIdentifier(entry.get(SUB_DEPARTMENT_IDENTIFIER_FIELD))
                   .withGroupIdentifier(entry.get(GROUP_IDENTIFIER_FIELD))
                   .build();
    }
}
