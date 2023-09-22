package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.pages.Pages;

import java.util.Set;

import static java.util.Objects.nonNull;

public class ArtBuilder extends AbstractPublicationInstanceBuilder {

    private static final Set<CristinSecondaryCategory> MUSICAL_WORK =
        Set.of(CristinSecondaryCategory.MUSICAL_PERFORMANCE,
            CristinSecondaryCategory.MUSICAL_PIECE);

    public ArtBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        CristinSecondaryCategory secondaryCategory = getCristinObject().getSecondaryCategory();
        if (CristinSecondaryCategory.FILM_PRODUCTION.equals(secondaryCategory)) {
            return createMovingPicture();
        } else if (isMusicalWork(secondaryCategory)) {
            return createMusicPerformance();
        }else if (CristinSecondaryCategory.VISUAL_ARTS.equals(secondaryCategory)) {
            return createVisualArts();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    private VisualArts createVisualArts() {
        return getCristinObject().getCristinProduct().toVisualArts();
    }

    private boolean isMusicalWork(CristinSecondaryCategory secondaryCategory) {
        return MUSICAL_WORK.contains(secondaryCategory);
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
