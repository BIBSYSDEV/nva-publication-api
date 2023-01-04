package no.unit.nva.cristin.mapper;

import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.JacocoGenerated;

public abstract class AbstractPublicationInstanceBuilder {

    public static final String ERROR_NOT_CORRECT_TYPE
        = "The cristin object can not be accepted by the %s constructor as it is not of type %s";
    private final CristinObject cristinObject;

    public AbstractPublicationInstanceBuilder(CristinObject cristinObject) {
        if (!isExpectedType(cristinObject)) {
            throw notExpectedType();
        }
        this.cristinObject = cristinObject;
    }

    public abstract PublicationInstance<? extends Pages> build();

    @JacocoGenerated
    public UnsupportedSecondaryCategoryException unknownSecondaryCategory() {
        return new UnsupportedSecondaryCategoryException();
    }

    @JacocoGenerated
    protected final boolean isExpectedType(CristinObject cristinObject) {
        return cristinObject.getMainCategory().equals(getExpectedType());
    }

    @JacocoGenerated
    protected abstract CristinMainCategory getExpectedType();

    protected final CristinObject getCristinObject() {
        return this.cristinObject;
    }

    @JacocoGenerated
    private IllegalStateException notExpectedType() {
        return new IllegalStateException(
            String.format(ERROR_NOT_CORRECT_TYPE, this.getClass().getSimpleName(), getExpectedType())
        );
    }
}
