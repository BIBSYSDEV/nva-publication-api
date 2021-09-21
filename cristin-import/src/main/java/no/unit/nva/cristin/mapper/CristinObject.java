package no.unit.nva.cristin.mapper;

import static java.util.Objects.nonNull;
import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@JsonIgnoreProperties({"type_mediebidrag", "brukernavn_opprettet", "peerReviewed",
    "brukernavn_siste_endring", "kildekode", "publiseringstatuskode", "merknadtekst_godkjenning",
    "dato_utgitt", "finansiering_varbeid", "varbeid_emneord", "type_produkt",
    "type_foredrag_poster", "kildepostid", "eierkode_opprettet", "arkivpost",
    "type_kunstneriskproduksjon", "type_utstilling", "pubidnr", "varbeid_kilde", "eierkode_siste_endring",
    "varbeid_vdisiplin", "arkivfil", "vitenskapeligarbeid_lokal",
    "merknadtekst", "dato_siste_endring"})
public class CristinObject implements JsonSerializable {

    public static final String PUBLICATION_OWNER_FIELD = "publicationOwner";
    public static final String MAIN_CATEGORY_FIELD = "varbeidhovedkatkode";
    public static final String SECONDARY_CATEGORY_FIELD = "varbeidunderkatkode";
    public static final String IDENTIFIER_ORIGIN = "Cristin";
    private static final ObjectMapper OBJECT_MAPPER_FAIL_ON_UNKNOWN =
        objectMapperWithEmpty.copy().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    public static final String BOOK_OR_REPORT_METADATA = "type_bok_rapport";

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("arstall")
    private Integer publicationYear;
    @JsonProperty("dato_opprettet")
    private LocalDate entryCreationDate;
    @JsonProperty("arstall_rapportert")
    private Integer yearReported;
    @JsonProperty("varbeid_sprak")
    private List<CristinTitle> cristinTitles;
    @JsonProperty(MAIN_CATEGORY_FIELD)
    private CristinMainCategory mainCategory;
    @JsonProperty(SECONDARY_CATEGORY_FIELD)
    private CristinSecondaryCategory secondaryCategory;
    @JsonProperty("varbeid_person")
    private List<CristinContributor> contributors;
    @JsonProperty("presentasjon_varbeid")
    private List<CristinPresentationalWork> presentationalWork;
    @JsonProperty("varbeid_emneord")
    private List<CristinTags> tags;
    @JsonProperty("varbeid_hrcs_klassifisering")
    private List<CristinHrcsCategoriesAndActiveties> hrcsCategoriesAndActiveties;
    @JsonProperty(BOOK_OR_REPORT_METADATA)
    private CristinBookOrReportMetadata bookOrReportMetadata;
    @JsonProperty("type_bok_rapport_del")
    private CristinBookOrReportPartMetadata bookOrReportPartMetadata;
    @JsonProperty("type_tidsskriftpublikasjon")
    private CristinJournalPublication journalPublication;
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

    public boolean isPeerReviewed() {
        return nonNull(yearReported);
    }

    public static CristinObject fromJson(JsonNode json) {
        return attempt(() -> OBJECT_MAPPER_FAIL_ON_UNKNOWN.convertValue(json, CristinObject.class)).orElseThrow();
    }
}
