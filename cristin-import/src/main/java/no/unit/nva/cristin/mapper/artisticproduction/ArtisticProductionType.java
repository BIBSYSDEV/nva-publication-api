package no.unit.nva.cristin.mapper.artisticproduction;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;



@Data
@Builder(
    builderClassName = "ArtisticProductionTypeBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"produksjonstypenavn", "produksjonstypenavn_engelsk", "produksjonstypenavn_nynorsk"})
public class ArtisticProductionType {

    @JsonProperty("produkttypekode")
    private String productTypeCode;

    public ArtisticProductionType() {

    }


}