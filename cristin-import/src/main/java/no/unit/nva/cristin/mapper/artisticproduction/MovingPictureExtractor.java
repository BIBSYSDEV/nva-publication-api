package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtype;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtypeEnum;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit.MINUTE;

public interface MovingPictureExtractor {

    int MAX_SHORT_FILM_LENGTH_IN_MINUTES = 40;

    default MovingPictureSubtype extractSubType(ArtisticProductionTimeUnit timeUnit, String duration) {
        return hasUnknownLenght(timeUnit, duration)
            ? MovingPictureSubtype.create(MovingPictureSubtypeEnum.OTHER)
            : determineShortOrFeatureFilm(timeUnit, duration);

    }


    private MovingPictureSubtype determineShortOrFeatureFilm(ArtisticProductionTimeUnit timeUnit, String duration) {
        return isShortFilm(timeUnit, duration)
            ? MovingPictureSubtype.create(MovingPictureSubtypeEnum.SHORT)
            : MovingPictureSubtype.create(MovingPictureSubtypeEnum.FILM);

    }


    private boolean hasUnknownLenght(ArtisticProductionTimeUnit timeUnit, String duration) {
        return isNull(timeUnit)
            || !timeUnit.timeUnitIsInMinutes()
            || isNull(duration);
    }

    @JsonIgnore
    private boolean isShortFilm(ArtisticProductionTimeUnit timeUnit, String duration) {
        return MINUTE.equalsIgnoreCase(timeUnit.getTimeUnitCode())
            && nonNull(duration)
            && Integer.parseInt(duration) <= MAX_SHORT_FILM_LENGTH_IN_MINUTES;
    }
}
