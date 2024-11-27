package no.unit.nva.model.associatedartifacts.file;

public interface PendingFile<A extends File, R extends File> {

    R reject();

    A approve();

    boolean isNotApprovable();
}
