package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.publication.s3imports.UriWrapper;
import nva.commons.core.JacocoGenerated;


import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_RESEARCH_PROJECT_NAME;

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

    private static final String API_URI_START = "https://api.";
    private static final String PROJECT = "project";
    private static final String DOMAIN_NAME = "DOMAIN_NAME";
    private static final String PROSJEKT = "PROSJEKT";

    @JsonProperty("presentasjonslopenr")
    private Integer identifier;
    @JsonProperty("presentasjonstypekode")
    private String presentationType;


    @JacocoGenerated
    public CristinPresentationalWorkBuilder copy() {
        return this.toBuilder();
    }

    public boolean isProject() {
        return presentationType.equals(PROSJEKT);
    }

    public ResearchProject toNvaResearchProject() {
        String domainName = System.getenv(DOMAIN_NAME);
        UriWrapper idUri = new UriWrapper(API_URI_START + domainName).addChild(PROJECT, identifier.toString());
        return new ResearchProject.Builder()
                .withId(idUri.getUri())
                .withName(HARDCODED_RESEARCH_PROJECT_NAME)
                .build();
    }
}
