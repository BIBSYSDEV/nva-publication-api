package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;


@Data
@Builder(
    builderClassName = "PerformanceBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"framforingstypenavn", "framforingstypenavn_engelsk", "framforingstypenavn_nynorsk"})
public class Performance {

    @JsonProperty("framforingstypekode")
    private String performanceType;

    @JacocoGenerated
    public Performance(){

    }
}
