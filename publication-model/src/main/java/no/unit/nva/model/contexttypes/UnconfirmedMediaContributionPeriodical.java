package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.exceptions.InvalidIssnException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnconfirmedMediaContributionPeriodical extends UnconfirmedJournal {

    public static final String TITLE_FIELD = "title";
    public static final String PRINT_ISSN_FIELD = "printIssn";
    public static final String ONLINE_ISSN_FIELD = "onlineIssn";

    @JsonCreator
    public UnconfirmedMediaContributionPeriodical(@JsonProperty(TITLE_FIELD) String title,
                                                  @JsonProperty(PRINT_ISSN_FIELD) String printIssn,
                                                  @JsonProperty(ONLINE_ISSN_FIELD) String onlineIssn)
            throws InvalidIssnException {
        super(title, printIssn, onlineIssn);
    }
}
