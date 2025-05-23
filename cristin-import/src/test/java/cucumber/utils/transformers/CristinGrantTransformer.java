package cucumber.utils.transformers;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.CristinGrant.GRANT_RERERENCE_FIELD;
import static no.unit.nva.cristin.mapper.CristinGrant.GRANT_SOURCE_CODE_ENGLISH_FIELD;
import static no.unit.nva.cristin.mapper.CristinGrant.GRANT_SOURCE_CODE_FIELD;
import static no.unit.nva.cristin.mapper.CristinGrant.IDENTIFIER_FIELD;
import static no.unit.nva.cristin.mapper.CristinGrant.YEAR_FROM_FIELD;
import static no.unit.nva.cristin.mapper.CristinGrant.YEAR_TO_FIELD;
import io.cucumber.java.DataTableType;
import java.util.Map;
import no.unit.nva.cristin.mapper.CristinGrant;

public class CristinGrantTransformer {

    private static final int CURRENTLY_MAPPED_FIELDS = 6;

    @DataTableType
    public CristinGrant toCristinGrant(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException("Wrong number of keys for cristinGrant");
        }
        var identifier = entry.get(IDENTIFIER_FIELD);
        var sourceCode = entry.get(GRANT_SOURCE_CODE_FIELD);
        var sourceCodeEnglish = entry.get(GRANT_SOURCE_CODE_ENGLISH_FIELD);
        var yearFrom = nonNull(entry.get(YEAR_FROM_FIELD)) ? Integer.parseInt(entry.get(YEAR_FROM_FIELD)) : null;
        var yearTo = nonNull(entry.get(YEAR_TO_FIELD)) ? Integer.parseInt(entry.get(YEAR_TO_FIELD)) : null;
        var grantReference = entry.get(GRANT_RERERENCE_FIELD);
        return CristinGrant.builder()
                   .withIdentifier(identifier)
                   .withSourceCode(sourceCode)
                   .withSourceCodeEnglish(sourceCodeEnglish)
                   .withYearFrom(yearFrom)
                   .withYearTo(yearTo)
                   .withGrantReference(grantReference)
                   .build();
    }
}
