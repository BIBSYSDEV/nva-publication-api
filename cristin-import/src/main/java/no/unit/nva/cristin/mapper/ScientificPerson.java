package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(builderClassName = "ScientificPersonBuilder", toBuilder = true, builderMethodName = "builder",
    buildMethodName = "build", setterPrefix = "with")
@Getter
@Setter
@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"rekkefolgenr", "fornavn", "etternavn", "institusjonsnavn", "avdnavn", "undavdnavn",
    "gruppenavn", "kjonn", "forfattere_sted", "faktortall_samarbeid", "forfatterandel",
    "forfattere_int", "faktortall_samarbeid_2003", "forfatterandel_2003", "forfattervekt_2003", "nsdstedkode",
    "institusjonskode", "eierkode", "status_rbo", "forfattere_totalt", "sektorkode", "status_int_samarbeid", "landkode",
    "landnavn", "landnavn_engelsk", "fodt_aar"})
public class ScientificPerson {

    public static final String AFFILIATION_DELIMITER = ".";
    private static final String RESOURCE_OWNER_FORMAT = "%s@%s";
    @JsonProperty("personlopenr")
    private String cristinPersonIdentifier;
    @JsonProperty("institusjonsnr")
    private String institutionIdentifier;
    @JsonProperty("avdnr")
    private String departmentIdentifier;
    @JsonProperty("undavdnr")
    private String subDepartmentIdentifier;
    @JsonProperty("gruppenr")
    private String groupIdentifier;
    @JsonProperty("forfattervekt")
    private String authorWeight;
    @JsonProperty("vektingstall")
    private String weightNumber;
    @JsonProperty("faktortall_samarbeid")
    private String cooperationNumber;

    @JacocoGenerated
    @JsonCreator
    private ScientificPerson() {

    }
}
