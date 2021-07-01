package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.Publication;
import nva.commons.core.JsonSerializable;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"brukernavn_opprettet","varbeid_vdisiplin","varbeid_hrcs_klassifisering","brukernavn_siste_endring",
        "kildekode","publiseringstatuskode","merknadtekst_godkjenning","arkivfil","dato_utgitt","type_bok_rapport_del",
        "type_kunstneriskproduksjon","varbeid_emneord","type_utstilling","kildepostid","eierkode_opprettet","vitenskapeligarbeid_lokal",
        "arkivpost","presentasjon_varbeid","pubidnr","eierkode_siste_endring","arstall_rapportert","varbeid_kilde","type_bok_rapport",
        "type_produkt","type_mediebidrag","finansiering_varbeid","type_foredrag_poster","merknadtekst","dato_siste_endring",
        "type_tidsskriftpublikasjon"})
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
    @JsonProperty
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
