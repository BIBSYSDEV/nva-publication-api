package no.unit.nva.cristin.mapper.exhibition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.cristin.mapper.nva.exceptions.CristinMuseumCategoryException;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtype;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtypeEnum;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionBasic;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionProductionManifestation;
import no.unit.nva.model.time.Period;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "CristinExhibitionBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"varbeidlopenr", "utstillingstype", "antall_besokende", "antall_gjenstander",
    "tall_andel_egne_gjenstander", "tall_brukt_areal", "belop_budsjett"})
public class CristinExhibition {

    @JsonIgnore
    private static final String YES = "J";

    @JsonProperty("hendelse")
    private ExhibitionEvent exhibitionEvent;

    @JsonProperty("status_permanent")
    private String statusPermanent;

    @JacocoGenerated
    public CristinExhibition() {

    }

    public ExhibitionProductionSubtype extractExhibitionSubtype() {
        return new ExhibitionProductionSubtype(determineExhibitionSubtypeEnum());
    }

    public ExhibitionProduction toExhibitionProduction() {
        if (!isExhibitionCategory()) {
            throw new CristinMuseumCategoryException(getCategory());
        }
        return new ExhibitionProduction(extractExhibitionSubtype(), extractExhibitionManifestation());
    }

    @JsonIgnore
    private String getCategory() {
        return exhibitionEvent.getMuseumEventCategory().getEventCode();
    }

    @JsonIgnore
    private boolean isExhibitionCategory() {
        return exhibitionEvent.getMuseumEventCategory().isMuseumExhibition();
    }

    private List<ExhibitionProductionManifestation> extractExhibitionManifestation() {
        return List.of(extractExhibitionBasic());
    }

    private ExhibitionBasic extractExhibitionBasic() {
        return new ExhibitionBasic(extractOrganisation(), extractPlace(), extractPeriod());
    }

    private UnconfirmedOrganization extractOrganisation() {
        return new UnconfirmedOrganization(null);
    }

    private UnconfirmedPlace extractPlace() {
        return new UnconfirmedPlace(null, null);
    }

    private Period extractPeriod() {
        return exhibitionEvent.toPeriod();
    }

    private ExhibitionProductionSubtypeEnum determineExhibitionSubtypeEnum() {
        return isPermanent()
                   ? ExhibitionProductionSubtypeEnum.BASIC_EXHIBITION
                   : ExhibitionProductionSubtypeEnum.TEMPORARY_EXHIBITION;
    }

    @JsonIgnore
    private boolean isPermanent() {
        return permanentStatusIsTrue() || exhibitionEvent.isInfiniteEvent();
    }

    @JsonIgnore
    private boolean permanentStatusIsTrue() {
        return YES.equals(statusPermanent);
    }
}
