package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "CristinMediaContributionBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"formidlingskanal", "kanalnavn", "tidsskrift"})
public class CristinMediaContribution {

    @JsonProperty("mediumtype")
    private CristinMediumType cristinMediumType;

    @JsonProperty("mediumstednavn")
    private String mediaPlaceName;

    @JacocoGenerated
    public CristinMediaContribution() {

    }

    @JacocoGenerated
    public CristinMediaContributionBuilder copy() {
        return this.toBuilder();
    }
}
