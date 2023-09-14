package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "CristinMediumTypeBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinMediumType {

    @JsonProperty("mediumtypekode")
    private CristinMediumTypeCode mediumTypeCode;

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
