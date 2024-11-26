package cucumber.features.transformers;

import static java.util.Objects.isNull;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.UNKNOWN;
import io.cucumber.java.DataTableType;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishedFileTransformer {

    private static final int CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS = 8;
    private static final Logger logger = LoggerFactory.getLogger(PublishedFileTransformer.class);
    private static final String WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS =
        String.format("This transformer maps only %d number of fields. Update the transformer to map more fields",
                      CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS);
    public static final String NULL_AS_STRING = "null";

    @DataTableType
    public static OpenFile toOpenFile(Map<String, String> entry) {
        if (entry.size() > CURRENTLY_MAX__NUMBER_OF_MAPPED_FIELDS) {
            throw new UnsupportedOperationException(WRONG_NUMBER_OF_FIELDS_FOR_CRISTIN_PRESENTATIONAL_WORKS);
        }
        var publisherVersion =
            NULL_AS_STRING.equalsIgnoreCase(entry.get("publisherVersion")) || isNull(entry.get("publisherVersion"))
                ? null
                : PublisherVersion.valueOf(entry.get("publisherVersion"));
        return new OpenFile(
            UUID.fromString(entry.get("identifier")),
            entry.get("filename"),
            entry.get("mimeType"),
            Long.parseLong(entry.get("size")),
            Optional.ofNullable(entry.get("license")).map(URI::create).orElse(null),
            publisherVersion,
            parseDate(entry.get("embargoDate")),
            parseRightsRetentionStrategy(entry),
            entry.getOrDefault("legalNote", null),
            parseDate(entry.get("publishedDate")),
            new UserUploadDetails(null, null)
        );
    }

    private static RightsRetentionStrategy parseRightsRetentionStrategy(Map<String, String> entry) {
        var rightsRetentionStrategy = Optional.ofNullable(entry.get("rightsRetentionStrategy"));
        if (rightsRetentionStrategy.isPresent()) {
            logger.warn("Rights retention strategy for Brage is not supported on record: " + entry.get("identifier"));
        }
        return NullRightsRetentionStrategy.create(UNKNOWN);
    }

    private static Instant parseDate(String candidate) {
        return isNullDate(candidate)
                   ? null
                   : Instant.parse(candidate);
    }

    private static boolean isNullDate(String candidate) {
        return StringUtils.isBlank(candidate) || NULL_AS_STRING.equalsIgnoreCase(candidate);
    }
}
