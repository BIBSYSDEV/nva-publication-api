package no.unit.nva.cristin.patcher.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

public class ParentAndChild implements JsonSerializable {

    @JsonProperty
    private Publication childPublication;

    @JsonProperty
    private Publication parentPublication;

    @JacocoGenerated
    public  ParentAndChild() {

    }

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
