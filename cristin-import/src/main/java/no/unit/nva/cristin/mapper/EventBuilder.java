package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isConferenceLecture;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isConferencePoster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isLecture;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isOtherPresentation;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.event.ConferenceLecture;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.event.OtherPresentation;
import no.unit.nva.model.pages.Pages;

public class EventBuilder extends AbstractPublicationInstanceBuilder {
    
    public EventBuilder(CristinObject cristinObject) {
        super(cristinObject);
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
        }
        if (isOtherPresentation(getCristinObject())) {
            return createOtherPresentation();
        } else {
            throw unknownSecondaryCategory();
        }
    }
    
    @Override
    protected CristinMainCategory getExpectedType() {
        return CristinMainCategory.EVENT;
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
    
    private PublicationInstance<? extends Pages> createOtherPresentation() {
        return new OtherPresentation();
    }
}
