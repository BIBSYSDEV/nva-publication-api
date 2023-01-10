package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isInterview;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.pages.Pages;

public class MediaContributionBuilder extends AbstractPublicationInstanceBuilder {

    public MediaContributionBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isInterview(getCristinObject())) {
            return createMediaInterview();
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
}
