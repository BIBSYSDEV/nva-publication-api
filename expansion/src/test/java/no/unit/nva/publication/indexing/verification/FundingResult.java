package no.unit.nva.publication.indexing.verification;

import static java.util.Objects.nonNull;
import java.net.URI;

public record FundingResult(
    String type,
    FundingSourceResult source,
    URI id,
    String identifier
) implements NoEmptyValues {

    @Override
    public boolean isNotEmpty() {
        return nonNull(type)
               && (("ConfirmedFunding".equals(type) && nonNull(id)) || "UnconfirmedFunding".equals(type))
               && nonNull(source)
               && source.isNotEmpty()
               && nonNull(identifier);
    }
}
