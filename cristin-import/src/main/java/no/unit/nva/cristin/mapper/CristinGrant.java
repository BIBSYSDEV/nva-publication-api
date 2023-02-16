package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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
}
