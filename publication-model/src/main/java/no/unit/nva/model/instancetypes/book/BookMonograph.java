package no.unit.nva.model.instancetypes.book;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static java.util.Objects.isNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class BookMonograph implements PublicationInstance<MonographPages> {

    public static final String PAGES_FIELD = "pages";
    private static final String CONTENT_TYPE_FIELD = "contentType";
    private final MonographPages pages;

    public BookMonograph(@JsonProperty(PAGES_FIELD) MonographPages pages) {
        this.pages = pages;
    }

    @JsonCreator
    public static BookMonograph fromJson(@JsonProperty(PAGES_FIELD) MonographPages pages,
                                         @JsonProperty(CONTENT_TYPE_FIELD) BookMonographContentType contentType) {
        if (BookMonographContentType.ACADEMIC_MONOGRAPH.equals(contentType)) {
            return new AcademicMonograph(pages);
        } else if (BookMonographContentType.ENCYCLOPEDIA.equals(contentType)) {
            return new Encyclopedia(pages);
        } else if (BookMonographContentType.EXHIBITION_CATALOG.equals(contentType)) {
            return new ExhibitionCatalog(pages);
        } else if (BookMonographContentType.NON_FICTION_MONOGRAPH.equals(contentType)) {
            return new NonFictionMonograph(pages);
        } else if (BookMonographContentType.POPULAR_SCIENCE_MONOGRAPH.equals(contentType)) {
            return new PopularScienceMonograph(pages);
        } else if (BookMonographContentType.TEXTBOOK.equals(contentType)) {
            return new Textbook(pages);
        } else if (isNull(contentType)) {
            return new AcademicMonograph(pages);
        } else {
            throw new UnsupportedOperationException("The Book Monograph subtype is unknown");
        }
    }

    @Override
    public MonographPages getPages() {
        return pages;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BookMonograph)) {
            return false;
        }
        BookMonograph that = (BookMonograph) o;
        return Objects.equals(getPages(), that.getPages());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPages());
    }
}
