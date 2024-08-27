package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Username;

@JsonTypeName(UserUploadDetails.TYPE)
@JsonTypeInfo(use = Id.NAME, property = "type")
public record UserUploadDetails(Username uploadedBy, Instant uploadedDate) implements UploadDetails, JsonSerializable {

    public static final String TYPE = "UserUploadDetails";

    //TODO: Remove method after migration
    @JsonProperty
    public String type() {
        return TYPE;
    }
}
