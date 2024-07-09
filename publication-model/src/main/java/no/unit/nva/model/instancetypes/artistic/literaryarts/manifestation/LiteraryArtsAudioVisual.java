package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.PublishingHouse;
import nva.commons.core.JacocoGenerated;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class LiteraryArtsAudioVisual implements LiteraryArtsManifestation {
    public static final String SUBTYPE_FIELD = "subtype";
    public static final String PUBLISHER_FIELD = "publisher";
    public static final String EXTENT_FIELD = "extent";
    public static final String ISBN_LIST_FIELD = "isbnList";
    public static final String PUBLICATION_DATE_FIELD = "publicationDate";
    @JsonProperty(SUBTYPE_FIELD)
    private final LiteraryArtsAudioVisualSubtype subtype;
    @JsonProperty(PUBLISHER_FIELD)
    private final PublishingHouse publisher;
    @JsonProperty(PUBLICATION_DATE_FIELD)
    private final PublicationDate publicationDate;
    @JsonAlias("isbn")
    @JsonProperty(ISBN_LIST_FIELD)
    private final List<String> isbnList;
    @JsonProperty(EXTENT_FIELD)
    private final Integer extent;

    @JsonCreator
    public LiteraryArtsAudioVisual(@JsonProperty(SUBTYPE_FIELD) LiteraryArtsAudioVisualSubtype subtype,
                                   @JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher,
                                   @JsonProperty(PUBLICATION_DATE_FIELD) PublicationDate publicationDate,
                                   @JsonProperty(ISBN_LIST_FIELD) Object isbnList,
                                   @JsonProperty(EXTENT_FIELD) Integer extent) {

        this.subtype = subtype;
        this.publisher = publisher;
        this.publicationDate = publicationDate;
        this.isbnList = migrateToList(isbnList);
        this.extent = extent;
    }

    @Deprecated
    private List<String> migrateToList(Object isbnList) {
        if (isNull(isbnList)) {
            return emptyList();
        } else if (isbnList instanceof String) {
            return List.of((String) isbnList);
        } else if (isbnList.getClass().isArray() || isbnList instanceof Collection) {
            return (List<String>) isbnList;
        } else {
            throw new IllegalArgumentException("ISBN List could not be parsed");
        }
    }

    public LiteraryArtsAudioVisualSubtype getSubtype() {
        return subtype;
    }

    public PublishingHouse getPublisher() {
        return publisher;
    }

    @Override
    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public List<String> getIsbnList() {
        return nonNull(isbnList) ? isbnList : emptyList();
    }

    public Integer getExtent() {
        return extent;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsAudioVisual)) {
            return false;
        }
        LiteraryArtsAudioVisual that = (LiteraryArtsAudioVisual) o;
        return Objects.equals(getSubtype(), that.getSubtype())
                && Objects.equals(getPublisher(), that.getPublisher())
                && Objects.equals(getPublicationDate(), that.getPublicationDate())
                && Objects.equals(getIsbnList(), that.getIsbnList())
                && Objects.equals(getExtent(), that.getExtent());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getSubtype(), getPublisher(), getPublicationDate(), getIsbnList(), getExtent());
    }
}
