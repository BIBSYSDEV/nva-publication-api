package no.unit.nva.model.contexttypes;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import no.unit.nva.model.contexttypes.utils.IssnUtil;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnconfirmedSeries implements BookSeries {

    public static final String TITLE = "title";
    public static final String ISSN = "issn";
    public static final String ONLINE_ISSN = "onlineIssn";
    @JsonProperty(TITLE)
    private final String title;
    @JsonProperty(ISSN)
    private final String issn;
    @JsonProperty(ONLINE_ISSN)
    private final String onlineIssn;

    @JsonCreator
    public UnconfirmedSeries(@JsonProperty(TITLE) String title,
                             @JsonProperty(ISSN) String issn,
                             @JsonProperty(ONLINE_ISSN) String onlineIssn) throws InvalidIssnException {
        this.title = title;
        this.issn = validateIssn(issn);
        this.onlineIssn = validateIssn(onlineIssn);
    }

    public static UnconfirmedSeries fromTitle(String title) {
        return attempt(() -> new UnconfirmedSeries(title, null, null)).orElseThrow();
    }

    @JacocoGenerated
    public String getOnlineIssn() {
        return onlineIssn;
    }

    @JacocoGenerated
    public String getIssn() {
        return issn;
    }

    @JacocoGenerated
    public String getTitle() {
        return title;
    }

    @Override
    public boolean isConfirmed() {
        return false;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getIssn(), getOnlineIssn());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnconfirmedSeries)) {
            return false;
        }
        UnconfirmedSeries that = (UnconfirmedSeries) o;
        return Objects.equals(getTitle(), that.getTitle())
               && Objects.equals(getIssn(), that.getIssn())
               && Objects.equals(getOnlineIssn(), that.getOnlineIssn());
    }

    private String validateIssn(String issn) throws InvalidIssnException {
        return StringUtils.isBlank(issn) ? null : IssnUtil.checkIssn(issn);
    }
}
