package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.artistic.architecture.Architecture;
import no.unit.nva.model.pages.Pages;

import java.util.Set;
import software.amazon.awssdk.services.s3.S3Client;

import static java.util.Objects.nonNull;

public class ArtBuilder extends AbstractPublicationInstanceBuilder {

    public static final Set<CristinSecondaryCategory> MUSICAL_WORK =
        Set.of(CristinSecondaryCategory.MUSICAL_PERFORMANCE,
            CristinSecondaryCategory.MUSICAL_PIECE);

    public ArtBuilder(CristinObject cristinObject, S3Client s3Client) {
        super(cristinObject, s3Client);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        CristinSecondaryCategory secondaryCategory = getCristinObject().getSecondaryCategory();
        if (CristinSecondaryCategory.FILM_PRODUCTION.equals(secondaryCategory)) {
            return createMovingPicture();
        } else if (isMusicalWork(secondaryCategory)) {
            return createMusicPerformance();
        } else if (CristinSecondaryCategory.VISUAL_ARTS.equals(secondaryCategory)) {
            return createVisualArts();
        } else if (CristinSecondaryCategory.THEATRICAL_PRODUCTION.equals(secondaryCategory)) {
            return createTheatricalProduction();
        } else if (CristinSecondaryCategory.ARCHITECT_DESIGN.equals(secondaryCategory)) {
            return createArchitecture();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    private Architecture createArchitecture() {
        return getCristinObject().getCristinProduct().toArchitecture();
    }

    private PerformingArts createTheatricalProduction() {
        return getCristinObject().getCristinArtisticProduction().toTheatricalPerformance();
    }

    private VisualArts createVisualArts() {
        return getCristinObject().getCristinProduct().toVisualArts();
    }

    private boolean isMusicalWork(CristinSecondaryCategory secondaryCategory) {
        return MUSICAL_WORK.contains(secondaryCategory);
    }

    private MusicPerformance createMusicPerformance() {
        var cristinObject = getCristinObject();
        return cristinObject.getCristinArtisticProduction().toMusicPerformance(cristinObject.getId(), super.getS3Client());
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
