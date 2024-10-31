package no.unit.nva.model.associatedartifacts.file;

public interface PendingFile<T> {

    RejectedFile reject();

    T approve();
}
