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
        builderClassName = "CristinJournalPublicationBuilder",
        toBuilder = true,
        builderMethodName = "builder",
        buildMethodName = "build",
        setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"antall_sider_totalt","publiseringsar", "status_referee_ordning",
    "supplement", "utbredelsesomrade", "artikkelnummer", "arstall_trykket", "arstall_online"})
public class CristinJournalPublication {

    public static final String JOURNAL = "tidsskrift";
    public static final String PAGES_BEGIN = "sidenr_fra";
    public static final String PAGES_END = "sidenr_til";
    public static final String VOLUME = "volum";
    public static final String ISSUE = "hefte";
    public static final String DOI = "doi";

    @JsonProperty(JOURNAL)
    private CristinJournalPublicationJournal journal;
    @JsonProperty(PAGES_BEGIN)
    private String pagesBegin;
    @JsonProperty(PAGES_END)
    private String pagesEnd;
    @JsonProperty(VOLUME)
    private String volume;
    @JsonProperty(ISSUE)
    private String issue;
    @JsonProperty(DOI)
    private String doi;

    public CristinJournalPublication() {

    }

    @JacocoGenerated
    public CristinJournalPublicationBuilder copy() {
        return this.toBuilder();
    }
}
