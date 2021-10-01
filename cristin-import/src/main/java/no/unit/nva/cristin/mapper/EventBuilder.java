package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.event.ConferenceLecture;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.pages.Pages;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isConferenceLecture;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isConferencePoster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isLecture;

public class EventBuilder extends AbstractPublicationInstanceBuilder {

    public EventBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    protected CristinMainCategory getExpectedType() {
        return CristinMainCategory.EVENT;
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isConferenceLecture(getCristinObject())) {
            return createConferenceLecture();
        }
        if (isConferencePoster(getCristinObject())) {
            return createConferencePoster();
        }
        if (isLecture(getCristinObject())) {
            return createLecture();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    private PublicationInstance<? extends Pages> createConferenceLecture() {
        return new ConferenceLecture();
    }

    private PublicationInstance<? extends Pages> createConferencePoster() {
        return new ConferencePoster();
    }

    private PublicationInstance<? extends Pages> createLecture() {
        return new Lecture();
    }
}
