package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;

//TODO: After migration remove JsonDeserialize annotation and uncomment commented annotations
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
//@JsonSubTypes({@JsonSubTypes.Type(name = "UserUploadDetails", value = UserUploadDetails.class),
//    @JsonSubTypes.Type(name = "ImportUploadDetails", value = ImportUploadDetails.class)})
@JsonDeserialize(using = UploadDetailsDeserializer.class)
public interface UploadDetails {

    Instant uploadedDate();
}
