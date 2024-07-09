package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import no.unit.nva.model.contexttypes.utils.IssnUtil;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnconfirmedJournal implements Periodical {
    private final String title;
    private final String printIssn;
    private final String onlineIssn;

    @JsonCreator
    public UnconfirmedJournal(@JsonProperty("title") String title,
                              @JsonProperty("printIssn") String printIssn,
                              @JsonProperty("onlineIssn") String onlineIssn) throws InvalidIssnException {
        this.title = title;
        this.printIssn = IssnUtil.checkIssn(printIssn);
        this.onlineIssn = IssnUtil.checkIssn(onlineIssn);
    }

    public String getTitle() {
        return title;
    }

    public String getPrintIssn() {
        return printIssn;
    }

    public String getOnlineIssn() {
        return onlineIssn;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnconfirmedJournal)) {
            return false;
        }
        UnconfirmedJournal that = (UnconfirmedJournal) o;
        return Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getPrintIssn(), that.getPrintIssn())
                && Objects.equals(getOnlineIssn(), that.getOnlineIssn());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getPrintIssn(), getOnlineIssn());
    }
}
