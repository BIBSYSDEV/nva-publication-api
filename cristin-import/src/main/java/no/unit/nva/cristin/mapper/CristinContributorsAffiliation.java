package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinContributorsAffiliation {

    @JsonProperty("institusjonsnr")
    private Integer institutionIdentifier;
    @JsonProperty("avdnr")
    private Integer departmentIdentifier;
    @JsonProperty("undavdnr")
    private Integer subdepartmentIdentifier;
    @JsonProperty("gruppenr")
    private Integer groupNumber;
    @JsonProperty("stedkode_opprinnelig")
    private String originalInsitutionCode;
    @JsonProperty("institusjonsnavn_opprinnelig")
    private String originalInstitutionName;
    @JsonProperty("avdelingsnavn_opprinnelig")
    private String originalDepartmentName;
    @JsonProperty("stednavn_opprinnelig")
    private String originalPlaceName; //TODO:  what is a place?
    @JsonProperty
    private List<CristinContributorRole> roles;

    public CristinContributorsAffiliation() {

    }
}
