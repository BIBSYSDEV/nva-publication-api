package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

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
    "avdnr_utgiver", "undavdnr_utgiver", "gruppenr_utgiver", "tidsskriftnr_serie",
    "sprakkode_oversatt_fra", "sprakkode_oversatt_til", "originalforfatter", "originaltittel",
    "arkivpost"})
public class CristinBookOrReportMetadata {
    
    public static final String ISBN_LIST = "isbn";
    public static final String PUBLISHER_NAME = "utgivernavn";
    public static final String NUMBER_OF_PAGES = "antall_sider_totalt";
    public static final String SUBJECT_FIELD = "fagfelt";
    public static final String SUBJECT_FIELD_IS_A_REQUIRED_FIELD =
        "The subjectField value must be present for all instances of Monography.";
    public static final String BOOK_SERIES = "tidsskrift_serie";
    public static final String ISSUE = "hefte";
    public static final String VOLUME = "volum_serie";
    public static final String PUBLISHER = "forlag";
    
    @JsonProperty(ISBN_LIST)
    private String isbn;
    @JsonProperty(PUBLISHER_NAME)
    private String publisherName;
    @JsonProperty(NUMBER_OF_PAGES)
    private String numberOfPages;
    @JsonProperty(SUBJECT_FIELD)
    private CristinSubjectField subjectField;
    @JsonProperty(BOOK_SERIES)
    //TODO rename class to something more appropriate.
    private CristinJournalPublicationJournal bookSeries;
    @JsonProperty(ISSUE)
    private String issue;
    @JsonProperty(VOLUME)
    private String volume;
    @JsonProperty(PUBLISHER)
    private CristinPublisher cristinPublisher;
    
    public CristinBookOrReportMetadata() {
    
    }
    
    public String getNumberOfPages() {
        return numberOfPages;
    }
    
    public String getPublisherName() {
        return publisherName;
    }
    
    @JacocoGenerated
    public CristinBookOrReportMetadata.CristinBookReportBuilder copy() {
        return this.toBuilder();
    }
}
