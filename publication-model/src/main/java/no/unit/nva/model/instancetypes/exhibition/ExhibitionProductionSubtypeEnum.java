package no.unit.nva.model.instancetypes.exhibition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ExhibitionProductionSubtypeEnum {
    BASIC_EXHIBITION("BasicExhibition"),
    TEMPORARY_EXHIBITION("TemporaryExhibition"),
    POPUP_EXHIBITION("PopupExhibition"),
    AMBULATING_EXHIBITION("AmbulatingExhibition"),
    DIGITAL_EXHIBITION("DigitalExhibition"),
    HISTORICAL_INTERIOR("HistoricalInterior"),
    OTHER("ExhibitionProductionOther");

    private final String type;

    ExhibitionProductionSubtypeEnum(String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return type;
    }



    // TODO: Remove following migration
    @Deprecated
    @JsonCreator
    public static ExhibitionProductionSubtypeEnum parse(String candidate) {
        return "Other".equalsIgnoreCase(candidate)
                ? ExhibitionProductionSubtypeEnum.OTHER
                : inlineableParseMethod(candidate);
    }

    public static ExhibitionProductionSubtypeEnum inlineableParseMethod(String candidate) {
        return Arrays.stream(values())
                .filter(item -> item.getType().equalsIgnoreCase(candidate))
                .collect(SingletonCollector.tryCollect())
                .orElseThrow(failure -> getFailure());
    }

    private static RuntimeException getFailure() {
        return new RuntimeException("Could not parse ExhibitionProductionSubtype, allowed values: "
                + getValuesString());
    }

    private static String getValuesString() {
        return Arrays.stream(values())
                .map(ExhibitionProductionSubtypeEnum::getType)
                .collect(Collectors.joining(", "));
    }
}
