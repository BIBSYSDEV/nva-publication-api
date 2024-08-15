package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;

@JsonDeserialize(using = UploadDetailsDeserializer.class)
public interface UploadDetails {

    Instant uploadedDate();
}
