package no.unit.nva.publication.indexing.verification;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.Map;

public record FundingSourceResult(URI id,
                                  String type,
                                  String identifier,
                                  Map<String, String> labels,
                                  Map<String, String> name) implements NoEmptyValues {

    @Override
    public boolean isNotEmpty() {
        return nonNull(id)
            && nonNull(type)
            && nonNull(identifier)
            && nonNull(labels)
            && !labels.isEmpty()
            && nonNull(name)
            && !name.isEmpty();
    }
}
