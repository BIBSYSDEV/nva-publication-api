package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@Deprecated
@JsonTypeName("PublicationNote")
public class PublicationNote extends PublicationNoteBase {

    @JsonCreator
    public PublicationNote(@JsonProperty("note") @JsonAlias({"publicationNoteMessage"}) String note) {
        super(note);
    }

}
