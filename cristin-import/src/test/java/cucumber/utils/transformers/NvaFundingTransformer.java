package cucumber.utils.transformers;

import static java.util.Objects.nonNull;
import io.cucumber.java.DataTableType;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;
import nva.commons.core.paths.UriWrapper;

public class NvaFundingTransformer {

    public static final String LABEL_FIELD = "label";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String ACTIVE_FROM_FIELD = "activeFrom";
    private static final String ACTIVE_TO_FIELD = "activeTo";
    private static final int CURRENTLY_MAPPED_FIELDS = 5;
    private static final String SOURCE_FIELD = "source";

    @DataTableType
    public Funding toNvaFunding(Map<String, String> entry) {
        if (entry.keySet().size() != CURRENTLY_MAPPED_FIELDS) {
            throw new UnsupportedOperationException("Wrong number of keys for nva funding");
        }
        var identifier = entry.get(IDENTIFIER_FIELD);
        var activeFrom = nonNull(entry.get(ACTIVE_FROM_FIELD)) ? Instant.parse(entry.get(ACTIVE_FROM_FIELD)) : null;
        var activeTo = nonNull(entry.get(ACTIVE_TO_FIELD)) ? Instant.parse(entry.get(ACTIVE_TO_FIELD)) : null;
        var source = UriWrapper.fromUri(entry.get(SOURCE_FIELD)).getUri();
        var labels = createLabels(entry.get(LABEL_FIELD));

        return new FundingBuilder().withIdentifier(identifier)
                   .withActiveFrom(activeFrom)
                   .withActiveTo(activeTo)
                   .withSource(source)
                   .withLabels(labels)
                   .build();
    }

    private Map<String, String> createLabels(String entry) {
        return "null".equalsIgnoreCase(entry)
                   ? Map.of()
                   : Map.of("en", entry, "nb", entry, "nn", entry);
    }
}
