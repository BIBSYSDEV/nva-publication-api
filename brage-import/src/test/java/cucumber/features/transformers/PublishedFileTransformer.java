package cucumber.features.transformers;

import io.cucumber.java.DataTableType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import org.junit.platform.commons.util.StringUtils;

public class PublishedFileTransformer {

    public static final boolean ADMINISTRATIVE_AGREEMENT_IS_ALWAYS_FALSE_FOR_PUBLISHED_FILES = false;
    private static final int CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS = 8;
    private static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                      CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS);

    @DataTableType
    public static PublishedFile toPublishedFile(Map<String, String> entry) {
        if (entry.keySet().size() > CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS);
        }
        var identifier = UUID.fromString(entry.get("identifier"));
        var fileName = entry.get("filename");
        var mimeType = entry.get("mimeType");
        var size = Long.parseLong(entry.get("size"));
        var license = entry.get("license");
        var publisherAuthority = Boolean.parseBoolean(entry.get("publisherAuthority"));
        var embargoDate = parseDate(entry.get("embargoDate"));
        var publishedDate = parseDate(entry.get("publishedDate"));
        return new PublishedFile(identifier,
                                 fileName,
                                 mimeType,
                                 size,
                                 license,
                                 ADMINISTRATIVE_AGREEMENT_IS_ALWAYS_FALSE_FOR_PUBLISHED_FILES,
                                 publisherAuthority,
                                 embargoDate,
                                 publishedDate
        );
    }

    private static Instant parseDate(String candidate) {
        return isNullDate(candidate)
                   ? null
                   : Instant.parse(candidate);
    }

    private static boolean isNullDate(String candidate) {
        return StringUtils.isBlank(candidate) || "null".equalsIgnoreCase(candidate);
    }
}
