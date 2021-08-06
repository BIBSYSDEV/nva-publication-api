package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import no.unit.nva.cristin.mapper.CristinTags;

import java.util.Map;

public class CristinTagsTransformer {

    public static final int CURRENTLY_MAPPED_FIELDS = 3;
    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TAGS =
            String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                    CURRENTLY_MAPPED_FIELDS);
    private static final String BOKMAL = "Bokmal";
    private static final String ENGLISH = "English";
    public static final String NYNORSK = "Nynorsk";

    @DataTableType
    public CristinTags toCristinTags(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TAGS);
        }
        String bokmal = entry.get(BOKMAL);
        String english = entry.get(ENGLISH);
        String nynrosk  = entry.get(NYNORSK);
        return CristinTags.builder()
                .withBokmal(bokmal)
                .withEnglish(english)
                .withNynorsk(nynrosk)
                .build();
    }
}
