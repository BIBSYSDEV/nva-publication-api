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
    builderClassName = "CristinBookReportPartBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"varbeidlopenr_inngar_i", "antall_sider_totalt",
    "status_utgitt_av_forlag", "delangivelse", "sprakkode_oversatt_fra", "doi", "fagfelt"})
public class CristinBookOrReportPartMetadata {
    
    public static final String PAGES_START = "sidenr_fra";
    public static final String PAGES_END = "sidenr_til";

    @JsonProperty(PAGES_START)
    private String pagesStart;
    @JsonProperty(PAGES_END)
    private String pagesEnd;

    @JacocoGenerated
    public CristinBookOrReportPartMetadata() {

    }

    public String getPagesStart() {
        return pagesStart;
    }

    public String getPagesEnd() {
        return pagesEnd;
    }

    @JacocoGenerated
    public CristinBookReportPartBuilder copy() {
        return this.toBuilder();
    }
}
