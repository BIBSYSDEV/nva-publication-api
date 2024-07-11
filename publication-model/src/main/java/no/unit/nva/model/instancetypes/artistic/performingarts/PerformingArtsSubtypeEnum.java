package no.unit.nva.model.instancetypes.artistic.performingarts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum PerformingArtsSubtypeEnum {
    THEATRICAL_PRODUCTION("TheatricalProduction"),
    BROADCAST("Broadcast"),
    OTHER("PerformingArtsOther");

    @JsonValue
    private final String type;

    PerformingArtsSubtypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    // TODO: Remove following migration
    @Deprecated
    @JsonCreator
    public static PerformingArtsSubtypeEnum parse(String candidate) {
        return "Other".equalsIgnoreCase(candidate)
                ? PerformingArtsSubtypeEnum.OTHER
                : inlineableParseMethod(candidate);
    }

    public static PerformingArtsSubtypeEnum inlineableParseMethod(String candidate) {
        return Arrays.stream(PerformingArtsSubtypeEnum.values())
                .filter(value -> value.getType().equals(candidate))
                .collect(SingletonCollector.collect());
    }
}
