package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "CristinBookReportPartBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"antall_sider_totalt", "status_utgitt_av_forlag", "delangivelse", "sprakkode_oversatt_fra"})
public class CristinBookOrReportPartMetadata {

    public static final String PAGES_START = "sidenr_fra";
    public static final String PAGES_END = "sidenr_til";
    public static final String PART_OF = "varbeidlopenr_inngar_i";
    private static final String DOI = "doi";
    public static final String SUBJECT_FIELD = "fagfelt";

    @JsonProperty(PAGES_START)
    private String pagesStart;
    @JsonProperty(PAGES_END)
    private String pagesEnd;

    @JsonProperty(PART_OF)
    private String partOf;

    @JsonProperty(DOI)
    private String doi;
    @JsonProperty(SUBJECT_FIELD)
    private CristinSubjectField subjectField;

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
