package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.mapper.CristinMapper.zoneOffset;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;
import nva.commons.core.paths.UriWrapper;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"belop"})
public class CristinGrant {

    public static final String IDENTIFIER_FIELD = "finansieringslopenr";
    public static final String GRANT_SOURCE_CODE_FIELD = "finansieringskildekode";
    public static final String GRANT_RERERENCE_FIELD = "finansieringsreferanse";
    public static final String YEAR_FROM_FIELD = "arstall_fra";
    public static final String YEAR_TO_FIELD = "arstall_til";
    public static final String ENGLISH_ISO_639_1 = "en";
    public static final String NORWEGIAN_BOKMAAL_ISO_639_1 = "nb";
    public static final String NORWEGIAN_NYNORSK_ISO_639_1 = "nn";
    public static final int FIRST_DAY_OF_MONTH = 1;
    public static final String CRISTIN = "cristin";
    public static final String FUNDING_SOURCES = "funding-sources";
    private static final String NFR_SOURCE_CODE = "NFR";
    private static final String VERIFIED_FUNDING_PATH = "verified-funding";
    @JsonProperty(IDENTIFIER_FIELD)
    private String identifier;

    @JsonProperty(GRANT_SOURCE_CODE_FIELD)
    private String sourceCode;

    @JsonProperty(GRANT_RERERENCE_FIELD)
    private String grantReference;

    @JsonProperty(YEAR_FROM_FIELD)
    private Integer yearFrom;

    @JsonProperty(YEAR_TO_FIELD)
    private Integer yearTo;

    @JsonIgnore
    public Funding toNvaFunding() {
        return new FundingBuilder().withIdentifier(identifier)
                   .withId(generateId())
                   .withLabels(generateLabels())
                   .withActiveFrom(convertDateToInstant(yearFrom))
                   .withActiveTo(convertDateToInstant(yearTo))
                   .withSource(generateSourceUri())
                   .build();
    }

    private URI generateSourceUri() {
        return UriWrapper.fromUri(NVA_API_DOMAIN)
                   .addChild(CRISTIN)
                   .addChild(FUNDING_SOURCES)
                   .addChild(urlEncode(sourceCode))
                   .getUri();
    }

    private Instant convertDateToInstant(Integer yearOrNull) {
        return Optional.ofNullable(yearOrNull).map(this::firstDayOfYear).orElse(null);
    }

    private Instant firstDayOfYear(Integer year) {
        return LocalDate.of(year, Month.JANUARY, FIRST_DAY_OF_MONTH).atStartOfDay().toInstant(zoneOffset());
    }

    private Map<String, String> generateLabels() {
        return Optional.ofNullable(grantReference)
                   .map(reference -> Map.of(ENGLISH_ISO_639_1, reference,
                                            NORWEGIAN_BOKMAAL_ISO_639_1, reference,
                                            NORWEGIAN_NYNORSK_ISO_639_1, reference))
                   .orElse(null);
    }

    private URI generateId() {
        return shouldHaveId()
                   ? UriWrapper.fromUri(NVA_API_DOMAIN)
                         .addChild(VERIFIED_FUNDING_PATH)
                         .addChild(sourceCode.toLowerCase(Locale.ROOT))
                         .addChild(identifier)
                         .getUri()
                   : null;
    }

    private boolean shouldHaveId() {
        return NFR_SOURCE_CODE.equalsIgnoreCase(sourceCode);
    }
}
