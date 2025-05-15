package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

public interface PendingFile<A extends File, R extends File> {

    String CANNOT_PUBLISH_FILE_MESSAGE = "Cannot publish a file without a license: %s";

    UUID getIdentifier();

    R reject();

    A approve();

    @JsonIgnore
    boolean isNotApprovable();
}
