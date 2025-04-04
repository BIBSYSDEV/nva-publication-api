package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_RESEARCH_PROJECT_NAME;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.model.ResearchProject;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@Builder(
    builderClassName = "CristinPresentationalWorkBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"personlopenr", "project"})

public class CristinPresentationalWork {

    public static final String PROSJEKT = "PROSJEKT";
    private static final String PROJECT = "project";
    private static final String CRISTIN_PATH = "cristin";

    @JsonProperty("presentasjonslopenr")
    private Integer identifier;
    @JsonProperty("presentasjonstypekode")
    private String presentationType;

    @JacocoGenerated
    public CristinPresentationalWork() {

    }

    @JacocoGenerated
    public CristinPresentationalWorkBuilder copy() {
        return this.toBuilder();
    }


    public boolean isProject() {
        return PROSJEKT.equals(presentationType);
    }

    public ResearchProject toNvaResearchProject() {
        return new ResearchProject.Builder()
                   .withId(constructId())
                   .withName(HARDCODED_RESEARCH_PROJECT_NAME)
                   .build();
    }

    private URI constructId() {
        return UriWrapper.fromUri(NVA_API_DOMAIN)
                   .addChild(CRISTIN_PATH,
                             PROJECT,
                             identifier.toString())
                   .getUri();
    }
}
