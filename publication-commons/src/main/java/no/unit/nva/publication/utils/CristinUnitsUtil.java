package no.unit.nva.publication.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Utility class for retrieving all Norwegian cristin units where you want to do repeated lookups and keep a large cache
 * like migrations and other bulk operations.
 */
public class CristinUnitsUtil {

    private static final Logger logger = LoggerFactory.getLogger(CristinUnitsUtil.class);
    private static final Map<String, CristinUnit> allUnits = new HashMap<>();
    private final S3Client s3Client;
    private final URI objectUri;

    public CristinUnitsUtil(S3Client s3Client,
                            String unitsS3ObjectUri) {
        this.s3Client = s3Client;
        this.objectUri = URI.create(unitsS3ObjectUri);
    }

    /***
     * Get the top level unit for a given unit.
     * @param unitId URI of the unit
     * @return URI of the top level unit or null if not found
     */
    public URI getTopLevel(URI unitId) {
        if (allUnits.isEmpty()) {
            loadAllData();
            buildTree();
            logger.info("Loaded cristin unit count: {}", allUnits.size());
        }

        var lookupId = UriWrapper.fromUri(unitId).getLastPathElement();

        return findTopLevelUnitOrSelf(lookupId).map(a -> URI.create(unitId.toString().replace(lookupId, a.id())))
                   .orElse(null);
    }

    private static void buildTree() {
        for (var unit : allUnits.values()) {
            if (nonNull(unit.parentUnit())) {
                var parent = allUnits.get(unit.parentUnit().id());
                if (parent.children() == null) {
                    parent = new CristinUnit(parent.id(), new ArrayList<>(), parent.parentUnit());
                }
                parent.children().add(unit);
            }
        }
    }

    private void loadAllData() {
        var s3Response = fetchUnitsFromS3();
        var units = attempt(() -> JsonUtils.dtoObjectMapper.readValue(s3Response, CristinUnit[].class)).orElseThrow();

        for (var unit : units) {
            allUnits.put(unit.id(), unit);
        }
    }

    private static Optional<CristinUnit> findTopLevelUnitOrSelf(String unitId) {
        var unit = CristinUnitsUtil.allUnits.get(unitId);
        if (isNull(unit)) {
            return Optional.empty();
        }
        while (nonNull(unit.parentUnit())) {
            unit = CristinUnitsUtil.allUnits.get(unit.parentUnit().id());
        }
        return Optional.of(unit);
    }

    private String fetchUnitsFromS3() {
        var s3uri = s3Client.utilities().parseUri(objectUri);
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(s3uri.bucket().orElseThrow())
                    .key(s3uri.key().orElseThrow())
                    .build())
                   .asUtf8String();
    }
}

/**
 * Represents a Cristin unit. JSON example: { "cristin_unit_id" : "5931.2.0.0", "unit_name" : { "en" : "Language Bank
 * and DH lab" }, "institution" : { "acronym" : "NB" }, "url" : "https://api.cristin.no/v2/units/5931.2.0.0", "acronym"
 * : "BS", "parent_unit" : { "cristin_unit_id" : "5931.0.0.0" } }
 */
@JsonInclude(Include.NON_NULL)
record CristinUnit(@JsonProperty(CRISTIN_UNIT_ID) String id,
                   ArrayList<CristinUnit> children,
                   @JsonProperty(PARENT_UNIT) CristinParentUnit parentUnit
)
    implements JsonSerializable {

    public static final String CRISTIN_UNIT_ID = "cristin_unit_id";
    public static final String PARENT_UNIT = "parent_unit";
}

@JsonInclude(Include.NON_NULL)
record CristinParentUnit(@JsonProperty("cristin_unit_id") String id) {

}
