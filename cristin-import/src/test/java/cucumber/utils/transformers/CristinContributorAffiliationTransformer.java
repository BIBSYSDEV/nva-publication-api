package cucumber.utils.transformers;

import static no.unit.nva.cristin.mapper.CristinContributorsAffiliation.DEPARTMENT_IDENTIFIER;
import static no.unit.nva.cristin.mapper.CristinContributorsAffiliation.GROUP_IDENTIFIER;
import static no.unit.nva.cristin.mapper.CristinContributorsAffiliation.INSITITUTION_IDENTIFIER;
import static no.unit.nva.cristin.mapper.CristinContributorsAffiliation.SUBDEPARTMENT_IDENTIFIER;
import io.cucumber.datatable.DataTable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;

public class CristinContributorAffiliationTransformer {

    public static List<CristinContributorsAffiliation> parseContributorAffiliationsFromMap(DataTable dataTable) {
        return dataTable.asMaps()
                   .stream()
                   .map(CristinContributorAffiliationTransformer::toCristinContributorAffiliation)
                   .collect(Collectors.toList());
    }

    public static CristinContributorsAffiliation toCristinContributorAffiliation(Map<String, String> entry) {
        return CristinContributorsAffiliation.builder()
                   .withInstitutionIdentifier(Integer.parseInt(entry.get(INSITITUTION_IDENTIFIER)))
                   .withDepartmentIdentifier(Integer.parseInt(entry.get(DEPARTMENT_IDENTIFIER)))
                   .withSubdepartmentIdentifier(Integer.parseInt(entry.get(SUBDEPARTMENT_IDENTIFIER)))
                   .withGroupNumber(Integer.parseInt(entry.get(GROUP_IDENTIFIER)))
                   .withRoles(CristinDataGenerator.randomAffiliation().getRoles())
                   .build();
    }
}
