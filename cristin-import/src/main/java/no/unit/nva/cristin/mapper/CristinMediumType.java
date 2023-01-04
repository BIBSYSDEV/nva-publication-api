package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "CristinMediumTypeBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinMediumType {

    @JsonProperty("mediumtypekode")
    private String mediumTypeCode;

    @JsonProperty("mediumtypenavn")
    private String mediumTypeNameNorwegianBokmaal;

    @JsonProperty("mediumtypenavn_engelsk")
    private String mediumTypeNameEnglish;

    @JsonProperty("mediumtypenavn_nynorsk")
    private String mediumTypeNameNorwegianNynorsk;

    @JacocoGenerated
    public CristinMediumType() {
        
    }
}
