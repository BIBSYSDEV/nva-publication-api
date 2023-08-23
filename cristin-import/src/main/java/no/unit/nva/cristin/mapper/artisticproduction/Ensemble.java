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
    builderClassName = "EnsembleBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"ensembletypenavn", "ensembletypenavn_engelsk", "ensembletypenavn_nynorsk"})
public class Ensemble {

    @JsonProperty("ensembletypekode")
    private String ensembleType;

    @JacocoGenerated
    public Ensemble(){

    }
}
