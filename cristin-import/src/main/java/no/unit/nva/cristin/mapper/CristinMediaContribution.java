package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "CristinMediaContributionBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"formidlingskanal", "kanalnavn"})
public class CristinMediaContribution {

    @JsonProperty("mediumtype")
    private CristinMediumType cristinMediumType;

    @JsonProperty("tidsskrift")
    private CristinJournalPublication journalPublication;

    @JsonProperty("mediumstednavn")
    private String mediaPlaceName;

    @JacocoGenerated
    public CristinMediaContribution() {

    }
}
