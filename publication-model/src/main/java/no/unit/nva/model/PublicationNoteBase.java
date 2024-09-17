package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(PublicationNote.class),
    @JsonSubTypes.Type(UnpublishingNote.class)
})
public class PublicationNoteBase {

    private String note;

    protected PublicationNoteBase(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public int hashCode() {
        return Objects.hash(note);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublicationNoteBase that = (PublicationNoteBase) o;
        return Objects.equals(note, that.note);
    }
}
