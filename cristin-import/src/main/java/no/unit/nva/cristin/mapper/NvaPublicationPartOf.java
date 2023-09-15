package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "NvaPublicationPartOfBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class NvaPublicationPartOf {

    @JsonProperty("cristinid")
    private String cristinId;

    private Publication parentPublication;

    @JacocoGenerated
    public NvaPublicationPartOf() {
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NvaPublicationPartOf that = (NvaPublicationPartOf) o;
        return Objects.equals(getCristinId(), that.getCristinId()) && Objects.equals(
            getParentPublication(), that.getParentPublication());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getCristinId(), getParentPublication());
    }
}
