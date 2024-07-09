package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.PublishingHouse;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class LiteraryArtsWeb implements LiteraryArtsManifestation {
    public static final String PUBLICATION_DATE_FIELD = "publicationDate";
    public static final String PUBLISHER_FIELD = "publisher";
    public static final String ID_FIELD = "id";
    @JsonProperty(ID_FIELD) private final URI id;
    @JsonProperty(PUBLISHER_FIELD) private final PublishingHouse publisher;
    @JsonProperty(PUBLICATION_DATE_FIELD) private final PublicationDate publicationDate;

    @JsonCreator
    public LiteraryArtsWeb(@JsonProperty(ID_FIELD) URI id,
                           @JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher,
                           @JsonProperty(PUBLICATION_DATE_FIELD) PublicationDate publicationDate) {
        this.id = id;
        this.publisher = publisher;
        this.publicationDate = publicationDate;
    }

    @Override
    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public URI getId() {
        return id;
    }

    public PublishingHouse getPublisher() {
        return publisher;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsWeb)) {
            return false;
        }
        LiteraryArtsWeb that = (LiteraryArtsWeb) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getPublisher(), that.getPublisher())
                && Objects.equals(getPublicationDate(), that.getPublicationDate());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getPublisher(), getPublicationDate());
    }
}
