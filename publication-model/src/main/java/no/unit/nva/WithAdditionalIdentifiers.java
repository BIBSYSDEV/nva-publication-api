package no.unit.nva;

import java.util.Set;
import no.unit.nva.model.AdditionalIdentifierBase;

public interface WithAdditionalIdentifiers {

    Set<AdditionalIdentifierBase> getAdditionalIdentifiers();

    void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers);
}
