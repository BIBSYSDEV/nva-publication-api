package cucumber.utils.transformers;

import io.cucumber.datatable.DataTable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.cristin.mapper.CristinSource;

public class CristinSourceTransformer {

    public static List<CristinSource> parseCristinSourceFromMap(DataTable dataTable) {
        return dataTable.asMaps().stream().map(CristinSourceTransformer::toCristinSource).collect(Collectors.toList());
    }

    public static CristinSource toCristinSource(Map<String, String> entry) {
        return CristinSource
                   .builder()
                   .withSourceCode(entry.get("Source Code Text"))
                   .withSourceIdentifier(entry.get("Source Identifier Text"))
                   .build();
    }
}
