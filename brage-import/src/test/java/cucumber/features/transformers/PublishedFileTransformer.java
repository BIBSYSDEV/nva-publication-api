package cucumber.features.transformers;

import io.cucumber.java.DataTableType;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishedFileTransformer {

    public static final boolean ADMINISTRATIVE_AGREEMENT_IS_ALWAYS_FALSE_FOR_PUBLISHED_FILES = false;
    private static final int CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS = 8;
    private static final Logger logger = LoggerFactory.getLogger(PublishedFileTransformer.class);
    private static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                      CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS);

    @DataTableType
    public static PublishedFile toPublishedFile(Map<String, String> entry) {
        if (entry.size() > CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS);
        }
        return new PublishedFile(
            UUID.fromString(entry.get("identifier")),
            entry.get("filename"),
            entry.get("mimeType"),
            Long.parseLong(entry.get("size")),
            entry.get("license"),
            ADMINISTRATIVE_AGREEMENT_IS_ALWAYS_FALSE_FOR_PUBLISHED_FILES,
            Boolean.parseBoolean(entry.get("publisherAuthority")),
            parseDate(entry.get("embargoDate")),
            parseRightsRetentionStrategy(entry),
            parseDate(entry.get("publishedDate"))
        );
    }

    private static RightsRetentionStrategy parseRightsRetentionStrategy(Map<String, String> entry) {
        var rightsRetentionStrategy = Optional.ofNullable(entry.get("rightsRetentionStrategy"));
        if (rightsRetentionStrategy.isPresent()) {
            logger.warn("Rights retention strategy for Brage is not supported on record: " + entry.get("identifier"));
        }
        return NullRightsRetentionStrategy.defaultRightsRetentionStrategy();
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
