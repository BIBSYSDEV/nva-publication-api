package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum LiteraryArtsAudioVisualSubtypeEnum {
    AUDIOBOOK("Audiobook"),
    RADIO_PLAY("RadioPlay"),
    SHORT_FILM("ShortFilm"),
    PODCAST("Podcast"),
    OTHER("LiteraryArtsAudioVisualOther");

    private final String type;

    LiteraryArtsAudioVisualSubtypeEnum(String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return type;
    }

    // TODO: Remove following migration
    @Deprecated
    @JsonCreator
    public static LiteraryArtsAudioVisualSubtypeEnum parse(String candidate) {
        return "Other".equalsIgnoreCase(candidate)
                ? LiteraryArtsAudioVisualSubtypeEnum.OTHER
                : inlineableParseMethod(candidate);
    }

    //  @JsonCreator
    public static LiteraryArtsAudioVisualSubtypeEnum inlineableParseMethod(String candidate) {
        return Arrays.stream(LiteraryArtsAudioVisualSubtypeEnum.values())
                .filter(value -> value.getType().equalsIgnoreCase(candidate))
                .collect(SingletonCollector.collect());
    }
}
