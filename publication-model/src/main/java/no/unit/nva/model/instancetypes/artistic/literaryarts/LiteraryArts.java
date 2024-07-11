package no.unit.nva.model.instancetypes.artistic.literaryarts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsManifestation;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class LiteraryArts implements PublicationInstance<NullPages> {

    public static final String DESCRIPTION_FIELD = "description";
    public static final String MANIFESTATIONS_FIELD = "manifestations";
    public static final String SUBTYPE_FIELD = "subtype";
    @JsonProperty(SUBTYPE_FIELD) private final LiteraryArtsSubtype subtype;
    @JsonProperty(MANIFESTATIONS_FIELD) private final List<LiteraryArtsManifestation> manifestations;
    @JsonProperty(DESCRIPTION_FIELD) private final String description;

    @JsonCreator
    public LiteraryArts(@JsonProperty(SUBTYPE_FIELD) LiteraryArtsSubtype subtype,
                        @JsonProperty(MANIFESTATIONS_FIELD) List<LiteraryArtsManifestation> manifestations,
                        @JsonProperty(DESCRIPTION_FIELD) String description) {

        this.subtype = subtype;
        this.manifestations = manifestations;
        this.description = description;
    }

    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }

    public LiteraryArtsSubtype getSubtype() {
        return subtype;
    }

    public List<LiteraryArtsManifestation> getManifestations() {
        return manifestations;
    }

    public String getDescription() {
        return description;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArts)) {
            return false;
        }
        LiteraryArts that = (LiteraryArts) o;
        return Objects.equals(getSubtype(), that.getSubtype())
                && Objects.equals(getManifestations(), that.getManifestations())
                && Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getSubtype(), getManifestations(), getDescription());
    }
}
