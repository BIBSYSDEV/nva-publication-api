package no.unit.nva.model.instancetypes.exhibition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionProductionManifestation;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionProductionManifestationList;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ExhibitionProduction implements PublicationInstance<NullPages> {

    public static final String SUBTYPE_FIELD = "subtype";
    public static final String MANIFESTATIONS_FIELD = "manifestations";
    @JsonProperty(SUBTYPE_FIELD)
    private final ExhibitionProductionSubtype subtype;
    @JsonProperty(MANIFESTATIONS_FIELD)
    private final ExhibitionProductionManifestationList manifestations;

    @JsonCreator
    public ExhibitionProduction(
            @JsonProperty(SUBTYPE_FIELD) ExhibitionProductionSubtype subtype,
            @JsonProperty(MANIFESTATIONS_FIELD) List<ExhibitionProductionManifestation> manifestations) {
        this.subtype = subtype;
        this.manifestations = new ExhibitionProductionManifestationList(manifestations);
    }

    public ExhibitionProductionSubtype getSubtype() {
        return subtype;
    }

    public List<ExhibitionProductionManifestation> getManifestations() {
        return manifestations;
    }

    @Override
    public NullPages getPages() {
        return new NullPages();
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExhibitionProduction)) {
            return false;
        }
        ExhibitionProduction that = (ExhibitionProduction) o;
        return Objects.equals(getSubtype(), that.getSubtype())
                && Objects.equals(getManifestations(), that.getManifestations());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getSubtype(), getManifestations());
    }
}
