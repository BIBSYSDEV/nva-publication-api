package no.unit.nva.cristin.mapper.exhibition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.cristin.mapper.DescriptionExtractor;
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
@JsonIgnoreProperties({"varbeidlopenr", "utstillingstype"})
public class CristinExhibition implements DescriptionExtractor {

    private static final String BUDGET_INFORMATION = "Beløp: %.1f NOK";
    private static final String AREA_INFORMATION = "Brukt areal: %.1f m2";
    private static final String NUMBER_OF_OBJECTS_INFORMATION = "Antall gjenstander: %d";
    private static final String NUMBER_OF_OWNED_OBJECTS_INFORMATION = "Andel egne gjenstander: %.1f%%";
    private static final String NUMBER_OF_VISITORS_INFORMATION = "Antall besøkende: %d";
    private static final String YES = "J";

    @JsonProperty("hendelse")
    private ExhibitionEvent exhibitionEvent;

    @JsonProperty("status_permanent")
    private String statusPermanent;

    @JsonProperty("belop_budsjett")
    private Double budget;

    @JsonProperty("tall_brukt_areal")
    private Double area;

    @JsonProperty("andel_egne_gjenstander")
    private Double percantageOfownedObjectsInExhibit;

    @JsonProperty("antall_gjenstander")
    private Integer numberOfObjectsInExhibit;

    @JsonProperty("antall_besokende")
    private Integer numberOfVisitors;

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
    public String getDescription() {
        var streams = Stream.of(exhibitionEvent.getDescription(),
                                createInformativeDescription(BUDGET_INFORMATION, budget),
                                createInformativeDescription(AREA_INFORMATION, area),
                                createInformativeDescription(NUMBER_OF_OBJECTS_INFORMATION,
                                                             numberOfObjectsInExhibit),
                                createInformativeDescription(NUMBER_OF_OWNED_OBJECTS_INFORMATION,
                                                             percantageOfownedObjectsInExhibit),
                                createInformativeDescription(NUMBER_OF_VISITORS_INFORMATION,
                                                             numberOfVisitors)
        );
        return extractDescription(streams);
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
        return exhibitionEvent.extractOrganisation();
    }

    private UnconfirmedPlace extractPlace() {
        return exhibitionEvent.extractPlace();
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
