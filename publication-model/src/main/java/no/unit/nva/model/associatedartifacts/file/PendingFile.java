package no.unit.nva.model.associatedartifacts.file;

import java.util.UUID;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;

public interface PendingFile<T> extends AssociatedArtifact {

    UUID getIdentifier();

    RejectedFile reject();

    T approve();
}
