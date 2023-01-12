package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isInterview;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isProgramParticipation;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.media.MediaParticipationInRadioOrTv;
import no.unit.nva.model.pages.Pages;

public class MediaContributionBuilder extends AbstractPublicationInstanceBuilder {

    public MediaContributionBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isInterview(getCristinObject())) {
            return createMediaInterview();
        } else if (isProgramParticipation(getCristinObject())) {
            return createProgramParticipation();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected CristinMainCategory getExpectedType() {
        return CristinMainCategory.MEDIA_CONTRIBUTION;
    }

    private static MediaInterview createMediaInterview() {
        return new MediaInterview();
    }

    private PublicationInstance<? extends Pages> createProgramParticipation() {
        return new MediaParticipationInRadioOrTv();
    }
}
