package no.unit.nva.model.instancetypes.artistic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.design.realization.Venue;
import no.unit.nva.model.pages.NullPages;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record OtherArtisticOutput(@JsonProperty(TYPE_DESCRIPTION_FIELD) String typeDescription,
                                  @JsonProperty(DESCRIPTION_FIELD) String description,
                                  @JsonProperty(VENUES_FIELD) Set<Venue> venues)
        implements PublicationInstance<NullPages> {
    private static final String TYPE_DESCRIPTION_FIELD = "typeDescription";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String VENUES_FIELD = "venues";

    public OtherArtisticOutput(String typeDescription, String description, Collection<Venue> venues) {
        this(typeDescription, description, new HashSet<>(venues));
    }

    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }
}
