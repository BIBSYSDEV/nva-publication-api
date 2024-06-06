package no.unit.nva.cristin.mapper.channelregistry;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChannelRegistryJournalByNameRepresentation {

    @CsvBindByName(column = "Original tittel")
    private String title;

    @CsvBindByName(column = "PID")
    private String pid;

    @CsvBindByName(column = "Print ISSN")
    private String printIssn;

    @CsvBindByName(column = "Online ISSN")
    private String onlineIssn;

    public boolean hasTitle(String value) {
        return title.equalsIgnoreCase(value);
    }

    public boolean hasIssn(String issn) {
        return hasPrintIssn(issn) || hasOnlineIssn(issn);
    }

    public boolean hasPrintIssn(String issn) {
        return printIssn.equalsIgnoreCase(issn);
    }

    public boolean hasOnlineIssn(String issn) {
        return onlineIssn.equalsIgnoreCase(issn);
    }
}
