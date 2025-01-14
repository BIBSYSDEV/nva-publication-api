package no.unit.nva.publication.permissions.file;

import no.unit.nva.model.FileOperation;

public interface FileDenyStrategy {
    boolean deniesAction(FileOperation permission);
}
