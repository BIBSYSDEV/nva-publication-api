package no.unit.nva.publication.indexing.verification;

import static java.util.Objects.nonNull;
import java.net.URI;

public record PublisherResult(URI id,
                              String type,
                              String identifier,
                              String name,
                              URI sameAs,
                              String scientificValue,
                              Boolean valid,
                              String year) implements NoEmptyValues {

    @Override
    public boolean isNotEmpty() {
        return nonNull(id)
               && nonNull(type)
               && nonNull(identifier)
               && nonNull(name)
               && nonNull(sameAs)
               && nonNull(scientificValue)
               && nonNull(valid)
               && nonNull(year);
    }
}
