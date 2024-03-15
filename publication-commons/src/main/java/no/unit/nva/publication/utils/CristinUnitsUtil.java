package no.unit.nva.publication.utils;

import static io.vavr.control.Try.of;
import static io.vavr.control.Try.ofSupplier;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.core.useragent.UserAgent;
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
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String API_HOST = "API_HOST";
    public static final String SIKT_EMIL = "support@sikt.no";
    public static final URI GITHUB_REPO = URI.create("https://github.com/BIBSYSDEV/nva-publication-api");
    private final URI apiUri;
    private final HttpClient httpClient;
    private final Environment environment;
    private final Class<?> caller;
    private final String cristinBotFilterBypassHeaderName;
    private final String cristinBotFilterBypassHeaderValue;

    public CristinUnitsUtil(HttpClient httpClient, URI apiUri, Environment environment, Class<?> caller,
                            String cristinBotFilterBypassHeaderName, String cristinBotFilterBypassHeaderValue) {
        this.httpClient = httpClient;
        this.apiUri = apiUri;
        this.environment = environment;
        this.caller = caller;
        this.cristinBotFilterBypassHeaderName = cristinBotFilterBypassHeaderName;
        this.cristinBotFilterBypassHeaderValue = cristinBotFilterBypassHeaderValue;
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
            String response = fetchResponseWithRetry(new URI(apiUri + API_ARGUMENTS + pageNum));
            return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, CristinUnit[].class)).orElseThrow();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchResponseWithRetry(URI requestUri) {
        var retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(5).intervalFunction(
            IntervalFunction.ofExponentialRandomBackoff()).build());
        var retryWithDefaultConfig = retryRegistry.retry("executeRequestAsync");
        Supplier<String> supplier = () -> executeRequest(requestUri);

        return ofSupplier(Retry.decorateSupplier(retryWithDefaultConfig, supplier)).get();
    }

    private String executeRequest(URI requestUri) {
        return of(() -> attempt(() -> httpClient.send(HttpRequest.newBuilder()
                                                          .uri(requestUri)
                                                          .headers(ACCEPT, APPLICATION_JSON,
                                                                   cristinBotFilterBypassHeaderName,
                                                                   cristinBotFilterBypassHeaderValue,
                                                                   USER_AGENT, getUserAgent())
                                                          .GET()
                                                          .build(),
                                                      BodyHandlers.ofString(StandardCharsets.UTF_8)))
                            .map(HttpResponse::body)
                            .toOptional().orElseThrow()).get();
    }

    private String getUserAgent() {
        return UserAgent.newBuilder().client(caller)
                   .environment(this.environment.readEnv(API_HOST))
                   .repository(GITHUB_REPO)
                   .email(SIKT_EMIL)
                   .version("1.0")
                   .build().toString();
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
