package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.pages.Pages;

import java.util.Set;

import static java.util.Objects.nonNull;

public class ArtBuilder extends AbstractPublicationInstanceBuilder {
    public ArtBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        CristinSecondaryCategory secondaryCategory = getCristinObject().getSecondaryCategory();
        if (CristinSecondaryCategory.FILM_PRODUCTION.equals(secondaryCategory)) {
            return createMovingPicture();
        }
        if (CristinSecondaryCategory.MUSICAL_PERFORMANCE.equals(secondaryCategory)) {
            return createMusicPerformance();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    private MusicPerformance createMusicPerformance() {
        return getCristinObject().getCristinArtisticProduction().toMusicPerformance();
    }

    private MovingPicture createMovingPicture() {
        if (nonNull(getCristinObject().getCristinArtisticProduction())) {
            return getCristinObject().getCristinArtisticProduction().toMovingPicture();
        } else {
            return getCristinObject().getCristinProduct().toMovingPicture();
        }
    }

    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.ARTISTIC_PRODUCTION);
    }
}
