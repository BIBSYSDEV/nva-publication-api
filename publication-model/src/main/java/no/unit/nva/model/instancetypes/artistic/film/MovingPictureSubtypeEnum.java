package no.unit.nva.model.instancetypes.artistic.film;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import nva.commons.core.SingletonCollector;

public enum MovingPictureSubtypeEnum {
  FILM("Film"),
  SHORT("ShortFilm"),
  SERIAL("SerialFilmProduction"),
  INTERACTIVE("InteractiveFilm"),
  AUGMENTED_VIRTUAL_REALITY("AugmentedVirtualRealityFilm"),
  OTHER("MovingPictureOther");

  @JsonValue private final String type;

  MovingPictureSubtypeEnum(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  // TODO: Remove following migration
  @Deprecated
  @JsonCreator
  public static MovingPictureSubtypeEnum parse(String candidate) {
    return "Other".equalsIgnoreCase(candidate)
        ? MovingPictureSubtypeEnum.OTHER
        : inlineableParseMethod(candidate);
  }

  //    @JsonCreator
  public static MovingPictureSubtypeEnum inlineableParseMethod(String candidate) {
    return Arrays.stream(MovingPictureSubtypeEnum.values())
        .filter(value -> value.getType().equalsIgnoreCase(candidate))
        .collect(SingletonCollector.collect());
  }
}
