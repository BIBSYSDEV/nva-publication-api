package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;


@Builder(
    builderClassName = "PerformanceBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"framforingstypenavn", "framforingstypenavn_engelsk", "framforingstypenavn_nynorsk"})
public class Performance {

    public static final String CONCERT = "KONSERT";
    @JsonProperty("framforingstypekode")
    private String performanceType;

    @JacocoGenerated
    public Performance() {

    }

    @JsonIgnore
    public boolean isConcert() {
        return  CONCERT.equals(performanceType);
    }
}
