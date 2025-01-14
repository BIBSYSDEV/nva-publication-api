package no.unit.nva.publication.permissions.file;

import no.unit.nva.model.FileOperation;

public interface FileGrantStrategy {
    boolean allowsAction(FileOperation permission);
}
