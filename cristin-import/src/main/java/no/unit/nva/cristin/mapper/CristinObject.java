package no.unit.nva.cristin.mapper;

import static java.util.Collections.emptyList;
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
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.artisticproduction.CristinProduct;
import no.unit.nva.cristin.mapper.exhibition.CristinExhibition;

@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
// This list should be emptied by either mapping the field to an NVA field or asking the Cristin people (Daniel)
// to remove it from the exports
@JsonIgnoreProperties({"brukernavn_opprettet", "peerreviewed",
    "brukernavn_siste_endring", "publiseringstatuskode", "merknadtekst_godkjenning",
    "arkivpost", "pubidnr", "eierkode_siste_endring",
    "varbeid_vdisiplin", "arkivfil", "dbh_forskres_kontroll"})
@SuppressWarnings({"PMD.TooManyFields", "PMD.CouplingBetweenObjects"})
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

    @JsonProperty("eierkode_opprettet")
    private String ownerCodeCreated;

    @JsonProperty("vitenskapeligarbeid_lokal")
    private List<CristinLocale> cristinLocales;

    @JsonProperty("h_dbh_forskres_publikasjon")
    private List<ScientificResource> scientificResources;

    @JsonProperty("institusjonsnr_opprettet")
    private String institutionIdentifierCreated;

    @JsonProperty("avdnr_opprettet")
    private String departmentIdentifierCreated;

    @JsonProperty("undavdnr_opprettet")
    private String subDepartmendIdentifierCreated;

    @JsonProperty("gruppenr_opprettet")
    private String groupIdentifierCreated;

    @JsonProperty("kildekode")
    private String sourceCode;

    @JsonProperty("kildepostid")
    private String sourceRecordIdentifier;

    @JsonProperty("finansiering_varbeid")
    private List<CristinGrant> cristinGrants;

    @JsonProperty("varbeid_url")
    private List<CristinAssociatedUri> cristinAssociatedUris;

    @JsonProperty("type_kunstneriskproduksjon")
    private CristinArtisticProduction cristinArtisticProduction;

    @JsonProperty("type_produkt")
    private CristinProduct cristinProduct;

    private String publicationOwner;

    @JsonProperty("merknadtekst")
    private String note;

    @JsonProperty("type_utstilling")
    private CristinExhibition cristinExhibition;

    public CristinObject() {
    }

    public static CristinObject fromJson(JsonNode json) {
        return attempt(() -> cristinEntryMapper.convertValue(json, CristinObject.class)).orElseThrow();
    }

    public CristinObjectBuilder copy() {
        return this.toBuilder();
    }

    public void hardcodePublicationOwner(String publicationsOwner) {
        this.setPublicationOwner(publicationsOwner);
    }

    @JsonProperty("peerreviewed")
    public boolean isPeerReviewed() {
        return nonNull(yearReported);
    }

    public List<CristinAssociatedUri> getCristinAssociatedUris() {
        return nonNull(cristinAssociatedUris) ? cristinAssociatedUris : emptyList();
    }
}
