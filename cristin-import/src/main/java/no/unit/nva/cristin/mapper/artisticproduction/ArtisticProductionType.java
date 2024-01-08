package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "ArtisticProductionTypeBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"produkttypenavn",
    "produksjonstypenavn",
    "produksjonstypenavn_engelsk",
    "produksjonstypenavn_nynorsk",
    "produkttypenavn_engelsk",
    "produkttypenavn_nynorsk"})
public class ArtisticProductionType {

    @JsonProperty("produkttypekode")
    @JsonAlias("produksjonstypekode")
    private String productTypeCode;

    @JacocoGenerated
    public ArtisticProductionType() {

    }
}
