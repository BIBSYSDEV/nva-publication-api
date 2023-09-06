package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;


@Builder(
    builderClassName = "EnsembleBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"ensembletypenavn", "ensembletypenavn_engelsk", "ensembletypenavn_nynorsk"})
public class Ensemble {

    @JsonProperty("ensembletypekode")
    private String ensembleType;

    @JacocoGenerated
    public Ensemble(){

    }
}
