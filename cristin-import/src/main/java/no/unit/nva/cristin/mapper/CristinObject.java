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
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
// This list should be emptied by either mapping the field to an NVA field or asking the Cristin people (Daniel)
// to remove it from the exports
@JsonIgnoreProperties({"brukernavn_opprettet", "peerreviewed",
    "brukernavn_siste_endring", "publiseringstatuskode", "merknadtekst_godkjenning",
    "finansiering_varbeid", "type_produkt",
    "kildepostid", "eierkode_opprettet", "arkivpost", "varbeid_url",
    "type_kunstneriskproduksjon", "type_utstilling", "pubidnr", "eierkode_siste_endring",
    "varbeid_vdisiplin", "arkivfil", "vitenskapeligarbeid_lokal", "merknadtekst", "h_dbh_forskres_publikasjon"})

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

    @JsonProperty("dato_utgitt")
    private LocalDate entryPublishedDate;

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

    @JsonProperty("type_mediebidrag")
    private CristinMediaContribution mediaContribution;

    @JsonProperty("varbeid_kilde")
    private List<CristinSource> cristinSources;

    @JsonProperty("kildekode")
    private String sourceCode;

    @JsonProperty("kildepostid")
    private String sourceRecordIdentifier;

    @JsonProperty("finansiering_varbeid")
    private List<CristinGrant> cristinGrants;

    private String publicationOwner;

    public CristinObject() {
    }

    public static CristinObject fromJson(JsonNode json) {
        return attempt(() -> cristinEntryMapper.convertValue(json, CristinObject.class)).orElseThrow();
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

    @JsonProperty("peerreviewed")
    public boolean isPeerReviewed() {
        return nonNull(yearReported);
    }
}
