package no.unit.nva.cristin.patcher.model;

import java.util.Objects;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

public class ParentAndChild {

    private final Publication childPublication;
    private final Publication parentPublication;

    public ParentAndChild(Publication childPublication, Publication parentPublication) {
        this.childPublication = childPublication;
        this.parentPublication = parentPublication;
    }


    public Publication getChildPublication() {
        return childPublication;
    }

    @JacocoGenerated
    public Publication getParentPublication() {
        return parentPublication;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getChildPublication(), getParentPublication());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParentAndChild)) {
            return false;
        }
        ParentAndChild that = (ParentAndChild) o;
        return Objects.equals(getChildPublication(), that.getChildPublication()) && Objects.equals(
            getParentPublication(), that.getParentPublication());
    }
}
