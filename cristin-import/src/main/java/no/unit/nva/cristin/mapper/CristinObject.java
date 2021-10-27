package no.unit.nva.cristin.mapper;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.CristinImportConfig.cristinEntryMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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
@JsonIgnoreProperties({"type_mediebidrag", "brukernavn_opprettet",
    "brukernavn_siste_endring", "kildekode", "publiseringstatuskode", "merknadtekst_godkjenning",
    "dato_utgitt", "finansiering_varbeid", "varbeid_emneord", "type_produkt",
    "kildepostid", "eierkode_opprettet", "arkivpost",
    "type_kunstneriskproduksjon", "type_utstilling", "pubidnr", "varbeid_kilde", "eierkode_siste_endring",
    "varbeid_vdisiplin", "arkivfil", "vitenskapeligarbeid_lokal", "merknadtekst"})

@SuppressWarnings({"PMD.TooManyFields"})
public class CristinObject implements JsonSerializable {

    public static final String PUBLICATION_OWNER_FIELD = "publicationOwner";
    public static final String MAIN_CATEGORY_FIELD = "varbeidhovedkatkode";
    public static final String SECONDARY_CATEGORY_FIELD = "varbeidunderkatkode";
    public static final String IDENTIFIER_ORIGIN = "Cristin";

    public static final String BOOK_OR_REPORT_METADATA = "type_bok_rapport";

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("arstall")
    private Integer publicationYear;
    @JsonProperty("dato_opprettet")
    private LocalDate entryCreationDate;
    @JsonProperty("dato_siste_endring")
    private LocalDate entryLastModifiedDate;
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
    private List<CristinHrcsCategoriesAndActivities> hrcsCategoriesAndActivities;
    @JsonProperty(BOOK_OR_REPORT_METADATA)
    private CristinBookOrReportMetadata bookOrReportMetadata;
    @JsonProperty("type_bok_rapport_del")
    private CristinBookOrReportPartMetadata bookOrReportPartMetadata;
    @JsonProperty("type_tidsskriftpublikasjon")
    private CristinJournalPublication journalPublication;
    @JsonProperty("type_foredrag_poster")
    private CristinLectureOrPosterMetaData lectureOrPosterMetaData;
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
        return attempt(() -> cristinEntryMapper.convertValue(json, CristinObject.class)).orElseThrow();
    }
}
