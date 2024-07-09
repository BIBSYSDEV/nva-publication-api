package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum LiteraryArtsPerformanceSubtypeEnum {
    READING("Reading"),
    PLAY("Play"),
    OTHER("LiteraryArtsPerformanceOther");

    private final String name;

    LiteraryArtsPerformanceSubtypeEnum(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    // TODO: Remove following migration
    @Deprecated
    @JsonCreator
    public static LiteraryArtsPerformanceSubtypeEnum parse(String candidate) {
        return "Other".equalsIgnoreCase(candidate)
                ? LiteraryArtsPerformanceSubtypeEnum.OTHER
                : inlineableParseMethod(candidate);
    }

    public static LiteraryArtsPerformanceSubtypeEnum inlineableParseMethod(String candidate) {
        return Arrays.stream(LiteraryArtsPerformanceSubtypeEnum.values())
                .filter(value -> value.getName().equals(candidate))
                .collect(SingletonCollector.collect());
    }
}
