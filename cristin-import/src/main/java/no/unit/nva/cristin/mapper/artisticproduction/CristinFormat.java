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
    builderClassName = "CristinFormatBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"formatnavn", "formatnavn_engelsk", "formatnavn_nynorsk"})
public class CristinFormat {

    @JsonProperty("formatkode")
    private String formatCode;

    @JacocoGenerated
    public CristinFormat() {

    }


}
