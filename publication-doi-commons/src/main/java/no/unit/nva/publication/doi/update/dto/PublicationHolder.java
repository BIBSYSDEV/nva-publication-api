package no.unit.nva.publication.doi.update.dto;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

public class PublicationHolder {

    private final String type;
    private final Publication item;
    private boolean isDoiUpdate;

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getItem());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationHolder)) {
            return false;
        }
        PublicationHolder that = (PublicationHolder) o;
        return Objects.equals(getType(), that.getType()) && Objects.equals(getItem(), that.getItem());
    }

    @JacocoGenerated
    @JsonCreator
    public PublicationHolder(
        @JsonProperty("type") String type,
        @JsonProperty("item") Publication publication) {
        this.type = type;
        this.item = publication;
    }

    public String getType() {
        return type;
    }

    public Publication getItem() {
        return item;
    }

    @JacocoGenerated
    public boolean isDoiUpdate() {
        if (nonNull(item) && nonNull(item.getDoiRequest())) {
            return item.getModifiedDate().equals(item.getDoiRequest().getModifiedDate());
        }
        return false;
    }

    @JacocoGenerated
    public void setDoiUpdate(boolean doiUpdate) {
        isDoiUpdate = doiUpdate;
    }
}
