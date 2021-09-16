package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;


@Data
@Builder(
    builderClassName = "CristinBookReportBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"utgave", "utgave_fra", "doi", "status_revidert", "status_elektronisk_publisert",
    "status_utgitt_av_forlag", "stedangivelse_utgiver", "landkode_utgiver", "institusjonsnr_utgiver",
    "avdnr_utgiver", "undavdnr_utgiver", "gruppenr_utgiver", "tidsskriftnr_serie", "volum_serie",
    "sprakkode_oversatt_fra", "sprakkode_oversatt_til", "originalforfatter", "originaltittel",
    "forlag",  "arkivpost"})
public class CristinBookOrReportMetadata {

    public static final String ISBN_LIST = "isbn";
    public static final String PUBLISHER = "utgivernavn";
    public static final String NUMBER_OF_PAGES = "antall_sider_totalt";
    public static final String SUBJECT_FIELD = "fagfelt";
    public static final String SUBJECT_FIELD_IS_A_REQUIRED_FIELD =
        "The subjectField value must be present for all instances of Monography.";
    public static final String BOOK_SERIES = "tidsskrift_serie";
    public static final String SEQUENTIAL_DESIGNATION = "hefte";

    @JsonProperty(ISBN_LIST)
    private String isbn;
    @JsonProperty(PUBLISHER)
    private String publisherName;
    @JsonProperty(NUMBER_OF_PAGES)
    private String numberOfPages;
    @JsonProperty(SUBJECT_FIELD)
    private CristinSubjectField subjectField;
    @JsonProperty(BOOK_SERIES)
    //TODO rename class to something more appropriate.
    private CristinJournalPublicationJournal bookSeries;
    @JsonProperty(SEQUENTIAL_DESIGNATION)
    public String sequentialDesignation;

    public CristinBookOrReportMetadata() {

    }

    public String getNumberOfPages() {
        return numberOfPages;
    }

    public String getPublisherName() {
        if (StringUtils.isBlank(publisherName)) {
            throw new InvalidCristinBookReportEntryException(PUBLISHER, publisherName);
        }
        return publisherName;
    }

    @JacocoGenerated
    public CristinBookOrReportMetadata.CristinBookReportBuilder copy() {
        return this.toBuilder();
    }

}
