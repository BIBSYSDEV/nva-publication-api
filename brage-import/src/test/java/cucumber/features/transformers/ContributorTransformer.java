package cucumber.features.transformers;

import io.cucumber.java.DataTableType;
import java.util.Map;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;

public class ContributorTransformer {

    private static final int CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS = 3;
    private static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                      CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS);

    @DataTableType
    public Contributor toContributor(Map<String, String> entry) {
        if (entry.size() > CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS);
        }
        var name = entry.get("name");
        var role = entry.get("role");
        var sequence = Integer.parseInt(entry.get("sequence"));
        var identity = new Identity.Builder()
                           .withName(name)
                           .build();

        return new Contributor.Builder()
                   .withIdentity(identity)
                   .withRole(new RoleType(Role.parse(role)))
                   .withSequence(sequence)
                   .build();
    }
}
