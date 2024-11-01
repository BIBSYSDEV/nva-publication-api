package no.unit.nva.model.associatedartifacts.file;

public interface PendingFile<T extends File> {

    RejectedFile reject();

    T approve();
}
