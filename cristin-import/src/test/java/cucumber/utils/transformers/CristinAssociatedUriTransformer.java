package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import no.unit.nva.cristin.mapper.CristinAssociatedUri;

import java.util.Map;

/**
 * DO NOT DELETE. In use by Cucumber.
 */
public class CristinAssociatedUriTransformer {


    private static final String URLTYPE_KODE_FIELD_NAME = "urltypekode";
    private static final String CRISTIN_URI_FIELD_NAME = "url";

    @DataTableType
    public static CristinAssociatedUri toCristinAssociatedUrl(Map<String, String> entry) {
        return new CristinAssociatedUri(entry.get(URLTYPE_KODE_FIELD_NAME),
            entry.get(CRISTIN_URI_FIELD_NAME));
    }
}
