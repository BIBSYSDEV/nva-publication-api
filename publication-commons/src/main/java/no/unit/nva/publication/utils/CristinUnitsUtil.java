package no.unit.nva.publication.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Get the top level unit for a given unit.
     * @param unitId URI of the unit
     * @return URI of the top level unit or null if not found
     */
    public URI getTopLevel(URI unitId) {
        if (isNull(unitId)) {
            throw new IllegalArgumentException("unitId cannot be null");
        }

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

