package no.unit.nva.publication.utils;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for retrieving all cristin units where you want to do repeated lookups and keep a large cache like
 * migrations and other bulk operations.
 */
public class CristinUnitsUtil {

    private static final Logger logger = LoggerFactory.getLogger(CristinUnitsUtil.class);
    private static final String API_ARGUMENTS = "?per_page=1000&country=NO&page=";
    private static final Map<String, CristinUnit> allUnits = new HashMap<>();

    public static final String APPLICATION_JSON = "application/json";
    private final URI apiUri;
    private final UriRetriever uriRetriever;

    public CristinUnitsUtil(UriRetriever uriRetriever, URI apiUri) {
        this.uriRetriever = uriRetriever;
        this.apiUri = apiUri;
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
        int pageNum = 1;

        while (true) {
            var units = getApiData(pageNum);
            if (units.length == 0) {
                break;
            } else {
                for (var unit : units) {
                    allUnits.put(unit.id(), unit);
                }
                pageNum++;
            }
        }
    }

    private static Optional<CristinUnit> findTopLevelUnitOrSelf(String unitId) {
        var unit = CristinUnitsUtil.allUnits.get(unitId);
        if (unit == null) {
            return Optional.empty();
        }
        while (unit.parentUnit() != null) {
            unit = CristinUnitsUtil.allUnits.get(unit.parentUnit().id());
        }
        return Optional.of(unit);
    }

    private CristinUnit[] getApiData(int pageNum) {
        try {
            String response =
                uriRetriever.getRawContent(new URI(apiUri + API_ARGUMENTS + pageNum), APPLICATION_JSON).orElseThrow();
            return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, CristinUnit[].class)).orElseThrow();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
