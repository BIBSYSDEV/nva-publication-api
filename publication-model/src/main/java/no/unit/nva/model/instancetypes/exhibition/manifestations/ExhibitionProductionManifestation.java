package no.unit.nva.model.instancetypes.exhibition.manifestations;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExhibitionCatalogReference.class, name = "ExhibitionCatalog"),
    @JsonSubTypes.Type(value = ExhibitionOtherPresentation.class, name = "ExhibitionOtherPresentation"),
    @JsonSubTypes.Type(value = ExhibitionBasic.class, name = "ExhibitionBasic"),
    @JsonSubTypes.Type(value = ExhibitionMentionInPublication.class, name = "ExhibitionMentionInPublication")
})
public interface ExhibitionProductionManifestation {
}
