package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.Publication;
import nva.commons.core.JsonSerializable;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"TYPE_MEDIEBIDRAG", "brukernavn_opprettet", "TYPE_TIDSSKRIFTPUBLIKASJON",
    "brukernavn_siste_endring", "kildekode", "publiseringstatuskode", "merknadtekst_godkjenning",
    "PRESENTASJON_VARBEID", "dato_utgitt", "FINANSIERING_VARBEID", "VARBEID_EMNEORD", "TYPE_PRODUKT",
    "TYPE_FOREDRAG_POSTER", "kildepostid", "TYPE_BOK_RAPPORT_DEL", "eierkode_opprettet", "ARKIVPOST",
    "TYPE_KUNSTNERISKPRODUKSJON", "TYPE_UTSTILLING", "pubidnr", "VARBEID_KILDE", "eierkode_siste_endring",
    "arstall_rapportert", "VARBEID_VDISIPLIN", "ARKIVFIL", "VITENSKAPELIGARBEID_LOKAL", "VARBEID_HRCS_KLASSIFISERING",
    "merknadtekst", "dato_siste_endring", "TYPE_BOK_RAPPORT"})
public class CristinObject implements JsonSerializable {

    public static final String PUBLICATION_OWNER_FIELD = "publicationOwner";
    public static final String MAIN_CATEGORY_FIELD = "varbeidhovedkatkode";
    public static final String SECONDARY_CATEGORY_FIELD = "varbeidunderkatkode";
    public static String IDENTIFIER_ORIGIN = "Cristin";

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("arstall")
    private String publicationYear;
    @JsonProperty("dato_opprettet")
    private LocalDate entryCreationDate;
    @JsonProperty("VARBEID_SPRAK")
    private List<CristinTitle> cristinTitles;
    @JsonProperty(MAIN_CATEGORY_FIELD)
    private CristinMainCategory mainCategory;
    @JsonProperty(SECONDARY_CATEGORY_FIELD)
    private CristinSecondaryCategory secondaryCategory;
    @JsonProperty("VARBEID_PERSON")
    private List<CristinContributor> contributors;
    private String publicationOwner;

    public CristinObject() {
    }

    public CristinObjectBuilder copy() {
        return this.toBuilder();
    }

    public Publication toPublication() {
        return new CristinMapper(this).generatePublication();
    }

    public void hardcodePublicationOwner(String publicationsOwner) {
        this.setPublicationOwner(publicationsOwner);
    }
}
