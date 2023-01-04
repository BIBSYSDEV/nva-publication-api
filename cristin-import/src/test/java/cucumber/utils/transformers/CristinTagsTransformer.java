package cucumber.utils.transformers;

import io.cucumber.java.DataTableType;
import java.util.Map;
import no.unit.nva.cristin.mapper.CristinTags;

public class CristinTagsTransformer {

    public static final int CURRENTLY_MAPPED_FIELDS = 3;
    public static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TAGS =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                      CURRENTLY_MAPPED_FIELDS);
    public static final String NYNORSK = "Nynorsk";
    private static final String BOKMAL = "Bokmal";
    private static final String ENGLISH = "English";

    @DataTableType
    public CristinTags toCristinTags(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_TAGS);
        }
        String bokmal = entry.get(BOKMAL);
        String english = entry.get(ENGLISH);
        String nynrosk = entry.get(NYNORSK);
        return CristinTags.builder()
                   .withBokmal(bokmal)
                   .withEnglish(english)
                   .withNynorsk(nynrosk)
                   .build();
    }
}
