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
    builderClassName = "CristinJournalPublicationJournalBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"tidsskriftnr", "landkode_utgiver", "sprakkode", "status_referee_ordning",
    "dato_utgatt", "status_serie"})
public class CristinJournalPublicationJournal {
    
    public static final String ISSN = "issn";
    public static final String ISSN_ONLINE = "issn_elektronisk";
    public static final String JOURNAL_TITLE = "tidsskriftnavn";
    public static final String NSD_JOURNAL_IDENTIFIER = "nsdkode";

    @JsonProperty(ISSN)
    private String issn;
    @JsonProperty(ISSN_ONLINE)
    private String issnOnline;
    @JsonProperty(JOURNAL_TITLE)
    private String journalTitle;
    @JsonProperty(NSD_JOURNAL_IDENTIFIER)
    private Integer nsdCode;

    public CristinJournalPublicationJournal() {

    }

    @JacocoGenerated
    public CristinJournalPublicationJournalBuilder copy() {
        return this.toBuilder();
    }
}
