package no.unit.nva.model.instancetypes.artistic.architecture;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum ArchitectureSubtypeEnum {
    BUILDING("Building"),
    PLANNING_PROPOSAL("PlanningProposal"),
    LANDSCAPE_ARCHITECTURE("LandscapeArchitecture"),
    INTERIOR("Interior"),
    OTHER("ArchitectureOther");

    @JsonValue
    private final String type;

    ArchitectureSubtypeEnum(String type) {
        this.type = type;
    }


    // TODO: Remove following migration
    @Deprecated
    @JsonCreator
    public static ArchitectureSubtypeEnum parse(String candidate) {
        return "Other".equalsIgnoreCase(candidate)
                ? ArchitectureSubtypeEnum.OTHER
                : inlineableParseMethod(candidate);
    }

    public static ArchitectureSubtypeEnum inlineableParseMethod(String candidate) {
        return Arrays.stream(ArchitectureSubtypeEnum.values())
                .filter(subType -> subType.getType().equalsIgnoreCase(candidate))
                .collect(SingletonCollector.collect());
    }

    public String getType() {
        return type;
    }
}
