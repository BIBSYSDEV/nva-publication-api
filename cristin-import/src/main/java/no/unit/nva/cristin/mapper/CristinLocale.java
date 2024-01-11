package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@Builder(
    builderClassName = "CristinLocaleBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"brukernavn_opprettet", "dato_opprettet", "brukernavn_siste_endring",
    "dato_siste_endring", "status_bekreftet_arkivsporsmal",
    "brukernavn_avlvrt_arkivsystem", "dato_avlvrt_arkivsystem", "status_fulgt_medf_reg",
    "brukernavn_svart_medforf_reg", "dato_svart_medforf_regel"})
public class CristinLocale {

    public static final String OWNER_CODE_FIELD = "eierkode";
    public static final String INSTITUTION_IDENTIFIER_FIELD = "institusjonsnr";
    public static final String DEPARTMENT_IDENTIFIER_FIELD = "avdnr";
    public static final String SUB_DEPARTMENT_IDENTIFIER_FIELD = "undavdnr";
    public static final String GROUP_IDENTIFIER_FIELD = "gruppenr";
    public static final String AFFILIATION_DELIMITER = ".";
    public static final String CRISTIN = "cristin";
    public static final String ORGANIZATION = "organization";
    private static final String RESOURCE_OWNER_FORMAT = "%s@%s";
    public static final String CONTROLLED_BY_FIELD = "brukernavn_kontrollert";
    public static final String DATE_CONTROLLED_FIELD = "dato_kontrollert";
    public static final String CONTROL_STATUS_FIELD = "status_kontrollert";
    @JsonProperty(OWNER_CODE_FIELD)
    private String ownerCode;

    @JsonProperty(INSTITUTION_IDENTIFIER_FIELD)
    private String institutionIdentifier;

    @JsonProperty(DEPARTMENT_IDENTIFIER_FIELD)
    private String departmentIdentifier;

    @JsonProperty(SUB_DEPARTMENT_IDENTIFIER_FIELD)
    private String subDepartmentIdentifier;

    @JsonProperty(GROUP_IDENTIFIER_FIELD)
    private String groupIdentifier;

    @JsonProperty(CONTROLLED_BY_FIELD)
    private String controlledBy;

    @JsonProperty(DATE_CONTROLLED_FIELD)
    private String dateControlled;

    @JsonProperty(CONTROL_STATUS_FIELD)
    private String controlStatus;


    @JacocoGenerated
    public CristinLocale() {

    }

    public ResourceOwner toResourceOwner() {
        return new ResourceOwner(new Username(extractOwner()), extractOwnerAffiliation());
    }

    private URI extractOwnerAffiliation() {
        return UriWrapper.fromUri(NVA_API_DOMAIN)
                   .addChild(CRISTIN)
                   .addChild(ORGANIZATION)
                   .addChild(completeAffiliation())
                   .getUri();
    }

    private String completeAffiliation() {
        return institutionIdentifier
               + AFFILIATION_DELIMITER
               + departmentIdentifier
               + AFFILIATION_DELIMITER
               + subDepartmentIdentifier
               + AFFILIATION_DELIMITER
               + groupIdentifier;
    }

    private String extractOwner() {
        return String.format(RESOURCE_OWNER_FORMAT,
                             ownerCode.toLowerCase(Locale.ROOT),
                             completeAffiliation());
    }
}
