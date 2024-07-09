package no.unit.nva.model.instancetypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Map implements PublicationInstance<MonographPages> {
    public static final String DESCRIPTION_FIELD = "description";
    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;
    @JsonProperty(PAGES_FIELD)
    private final MonographPages pages;

    public Map(@JsonProperty(DESCRIPTION_FIELD) String description,
               @JsonProperty(PAGES_FIELD) MonographPages pages) {
        this.description = description;
        this.pages = pages;
    }

    @Override
    public MonographPages getPages() {
        return pages;
    }

    public String getDescription() {
        return description;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map map = (Map) o;
        return Objects.equals(getDescription(), map.getDescription())
                && Objects.equals(getPages(), map.getPages());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getPages());
    }
}
