package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.file.File;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record FileForApproval(UUID identifier) {

    public static FileForApproval fromFile(File file) {
        return new FileForApproval(file.getIdentifier());
    }
}
