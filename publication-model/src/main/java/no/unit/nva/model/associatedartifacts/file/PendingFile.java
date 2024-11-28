package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface PendingFile<A extends File, R extends File> {

    R reject();

    A approve();

    @JsonIgnore
    boolean isNotApprovable();
}
