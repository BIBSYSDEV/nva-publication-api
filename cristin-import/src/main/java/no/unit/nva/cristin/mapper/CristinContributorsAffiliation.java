package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.cristin.lambda.constants.MappingConstants;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.s3imports.UriWrapper;

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

    public static final String CRISTIN_UNITS_DELIMITER = ".";
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
    @JsonProperty("VARBEID_PERSON_STED_ROLLE")
    private List<CristinContributorRole> roles;

    public CristinContributorsAffiliation() {

    }

    public Organization toNvaOrganization() {
        return new Organization.Builder()
                   .withId(buildId())
                   .withLabels(Collections.emptyMap())
                   .build();
    }

    public CristinObjectBuilder copy() {
        return this.toBuilder();
    }

    private URI buildId() {
        String affiliationCristinCode = String.join(CRISTIN_UNITS_DELIMITER,
                                                    institutionIdentifier.toString(),
                                                    departmentIdentifier.toString(),
                                                    subdepartmentIdentifier.toString(),
                                                    groupNumber.toString());
        return new UriWrapper(MappingConstants.CRISTIN_ORG_URI)
                   .addChild(affiliationCristinCode)
                   .getUri();
    }
}
