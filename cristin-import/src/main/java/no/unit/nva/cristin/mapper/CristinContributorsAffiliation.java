package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.CRISTIN_PATH;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.ORGANIZATION_PATH;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.Organization;
import nva.commons.core.paths.UriWrapper;

@Data
@Builder(
    builderClassName = "CristinContributorsAffiliationBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"stednavn_opprinnelig", "avdelingsnavn_opprinnelig", "institusjonsnavn_opprinnelig",
    "stedkode_opprinnelig"})
public class CristinContributorsAffiliation {

    public static final String CRISTIN_UNITS_DELIMITER = ".";
    public static final String INSITITUTION_IDENTIFIER = "institusjonsnr";
    public static final String DEPARTMENT_IDENTIFIER = "avdnr";
    public static final String SUBDEPARTMENT_IDENTIFIER = "undavdnr";
    public static final String GROUP_IDENTIFIER = "gruppenr";
    @JsonProperty(INSITITUTION_IDENTIFIER)
    private Integer institutionIdentifier;
    @JsonProperty(DEPARTMENT_IDENTIFIER)
    private Integer departmentIdentifier;
    @JsonProperty(SUBDEPARTMENT_IDENTIFIER)
    private Integer subdepartmentIdentifier;
    @JsonProperty(GROUP_IDENTIFIER)
    private Integer groupNumber;
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

    public CristinContributorsAffiliationBuilder copy() {
        return this.toBuilder();
    }

    private URI buildId() {
        String affiliationCristinCode = String.join(CRISTIN_UNITS_DELIMITER,
                                                    institutionIdentifier.toString(),
                                                    departmentIdentifier.toString(),
                                                    subdepartmentIdentifier.toString(),
                                                    groupNumber.toString());
        return UriWrapper.fromUri(NVA_API_DOMAIN)
                   .addChild(CRISTIN_PATH)
                   .addChild(ORGANIZATION_PATH)
                   .addChild(affiliationCristinCode)
                   .getUri();
    }
}
