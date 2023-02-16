package cucumber.utils.transformers;

import static java.util.Objects.nonNull;
import io.cucumber.java.DataTableType;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;

public class NvaFundingTransformer {

    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String ACTIVE_FROM_FIELD = "activeFrom";
    private static final String ACTIVE_TO_FIELD = "activeTo";
    private static final int CURRENTLY_MAPPED_FIELDS = 3;

    @DataTableType
    public Funding toNvaFunding(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException("Wrong number of keys for nva funding");
        }
        var identifier = entry.get(IDENTIFIER_FIELD);
        var activeFrom = nonNull(entry.get(ACTIVE_FROM_FIELD)) ? Instant.parse(entry.get(ACTIVE_FROM_FIELD)) : null;
        var activeTo = nonNull(entry.get(ACTIVE_TO_FIELD)) ? Instant.parse(entry.get(ACTIVE_TO_FIELD)) : null;

        return new FundingBuilder().withIdentifier(identifier)
                   .withActiveFrom(activeFrom)
                   .withActiveTo(activeTo)
                   .build();
    }
}
