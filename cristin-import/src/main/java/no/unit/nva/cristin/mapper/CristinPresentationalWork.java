package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_RESEARCH_PROJECT_NAME;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.publication.s3imports.UriWrapper;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "CristinPresentationalWorkBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"personlopenr"})

public class CristinPresentationalWork {

    private static final String PROJECT = "project";
    private static final String PROSJEKT = "PROSJEKT";

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
        return presentationType.equals(PROSJEKT);
    }

    public ResearchProject toNvaResearchProject() {

        UriWrapper idUri = new UriWrapper(NVA_API_DOMAIN).addChild(PROJECT, identifier.toString());
        return new ResearchProject.Builder()
            .withId(idUri.getUri())
            .withName(HARDCODED_RESEARCH_PROJECT_NAME)
            .build();
    }
}
