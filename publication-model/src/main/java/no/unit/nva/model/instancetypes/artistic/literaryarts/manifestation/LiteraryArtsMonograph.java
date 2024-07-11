package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class LiteraryArtsMonograph implements LiteraryArtsManifestation {
    public static final String PUBLISHER_FIELD = "publisher";
    public static final String DATE_FIELD = "publicationDate";
    public static final String ISBN_LIST_FIELD = "isbnList";
    public static final String PAGES_FIELD = "pages";
    @JsonProperty(PUBLISHER_FIELD) private final PublishingHouse publisher;
    @JsonProperty(DATE_FIELD) private final PublicationDate publicationDate;
    @JsonAlias("isbn")
    @JsonProperty(ISBN_LIST_FIELD) private final List<String> isbnList;
    @JsonProperty(PAGES_FIELD) private final MonographPages pages;

    // TODO: Remove flipping of String to List
    @JsonCreator
    public LiteraryArtsMonograph(@JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher,
                                 @JsonProperty(DATE_FIELD) PublicationDate publicationDate,
                                 @JsonProperty(ISBN_LIST_FIELD) Object isbnList,
                                 @JsonProperty(PAGES_FIELD) MonographPages pages) {
        this.publisher = publisher;
        this.publicationDate = publicationDate;
        this.isbnList = migrateToList(isbnList);
        this.pages = pages;
    }

    @Deprecated
    private List<String> migrateToList(Object isbnList) {
        if (isNull(isbnList)) {
            return Collections.emptyList();
        } else if (isbnList instanceof String) {
            return List.of((String) isbnList);
        } else if (isbnList.getClass().isArray() || isbnList instanceof Collection) {
            return (List<String>) isbnList;
        } else {
            throw new IllegalArgumentException("ISBN List could not be parsed");
        }
    }

    public PublishingHouse getPublisher() {
        return publisher;
    }

    public List<String> getIsbnList() {
        return nonNull(isbnList) ? isbnList : Collections.emptyList();
    }

    public MonographPages getPages() {
        return pages;
    }

    @Override
    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsMonograph)) {
            return false;
        }
        LiteraryArtsMonograph that = (LiteraryArtsMonograph) o;
        return Objects.equals(getPublisher(), that.getPublisher())
                && Objects.equals(getPublicationDate(), that.getPublicationDate())
                && Objects.equals(getIsbnList(), that.getIsbnList())
                && Objects.equals(getPages(), that.getPages());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPublisher(), getPublicationDate(), getIsbnList(), getPages());
    }
}
