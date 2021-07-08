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
@JsonIgnoreProperties({"hefte", "utgave", "utgave_fra", "doi", "status_revidert", "status_elektronisk_publisert",
        "status_utgitt_av_forlag", "stedangivelse_utgiver", "landkode_utgiver", "institusjonsnr_utgiver",
        "avdnr_utgiver", "undavdnr_utgiver", "gruppenr_utgiver", "tidsskriftnr_serie", "volum_serie",
        "sprakkode_oversatt_fra", "sprakkode_oversatt_til", "originalforfatter", "originaltittel",
        "forlag", "fagfelt"})
public class CristinBookReport {

    public static final String ISBN_LIST = "isbn";
    public static final String PUBLISHER = "utgivernavn";
    public static final String NUMBER_OF_PAGES = "antall_sider_totalt";

    @JsonProperty(ISBN_LIST)
    private String isbn;
    @JsonProperty(PUBLISHER)
    private String publisherName;
    @JsonProperty(NUMBER_OF_PAGES)
    private String numberOfPages;

    public CristinBookReport() {

    }

    public String getNumberOfPages() {
        if (StringUtils.isBlank(numberOfPages)) {
            throw new InvalidCristinBookReportEntryException(NUMBER_OF_PAGES, numberOfPages);
        }
        return numberOfPages;
    }

    @JacocoGenerated
    public CristinBookReport.CristinBookReportBuilder copy() {
        return this.toBuilder();
    }

}
