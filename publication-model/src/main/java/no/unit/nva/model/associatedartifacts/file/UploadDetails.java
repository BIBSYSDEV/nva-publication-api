package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = "UserUploadDetails", value = UserUploadDetails.class),
    @JsonSubTypes.Type(name = "ImportUploadDetails", value = ImportUploadDetails.class)})
public interface UploadDetails {

    Instant uploadedDate();
}
