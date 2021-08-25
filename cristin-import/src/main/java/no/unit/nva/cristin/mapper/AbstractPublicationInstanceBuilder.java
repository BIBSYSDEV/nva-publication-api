package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.JacocoGenerated;

public abstract class AbstractPublicationInstanceBuilder {

    private final CristinObject cristinObject;
    public static final String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";
    public static final String ERROR_NOT_CORRECT_TYPE
        = "The cristin object can not be accepted by the %s constructor as it is not of type %s";

    public AbstractPublicationInstanceBuilder(CristinObject cristinObject) {
        if (!isExpectedType(cristinObject)) {
            throw notExpectedType();
        }
        this.cristinObject = cristinObject;
    }

    public abstract PublicationInstance<? extends Pages> build();

    @JacocoGenerated
    public UnsupportedOperationException unknownSecondaryCategory() {
        return new UnsupportedOperationException(ERROR_PARSING_SECONDARY_CATEGORY);
    }

    @JacocoGenerated
    protected final boolean isExpectedType(CristinObject cristinObject) {
        return cristinObject.getMainCategory().equals(getExpectedType());
    }

    @JacocoGenerated
    protected abstract  CristinMainCategory getExpectedType();

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
