package no.unit.nva;

import java.util.Set;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;

public interface WithAdditionalIdentifiers {

    Set<AdditionalIdentifierBase> getAdditionalIdentifiers();

    void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers);
}
