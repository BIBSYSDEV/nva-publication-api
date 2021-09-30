package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.event.ConferenceLecture;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;

import java.util.Optional;

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
        return new ConferenceLecture(createMonographPages());
    }

    private PublicationInstance<? extends Pages> createConferencePoster() {
        return new ConferencePoster(createMonographPages());
    }

    private PublicationInstance<? extends Pages> createLecture() {
        return new Lecture(createMonographPages());
    }

    private MonographPages createMonographPages() {
        return new MonographPages.Builder()
                .withPages(extractNumberOfPages())
                .build();
    }

    private String extractNumberOfPages() {
        return Optional.ofNullable(getCristinObject().getLectureOrPosterMetaData().getNumberOfPages()).orElse(null);
    }
}
