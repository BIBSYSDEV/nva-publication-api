package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "NvaPublicationPartOfCristinPublicationBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class NvaPublicationPartOfCristinPublication implements JsonSerializable {

    @JsonProperty("nvapublicationidentifier")
    private String nvaPublicationIdentifier;

    @JsonProperty("childpublication")
    private Publication childPublication;

    @JsonProperty("partof")
    private NvaPublicationPartOf partOf;

    @JacocoGenerated
    public NvaPublicationPartOfCristinPublication() {
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
        NvaPublicationPartOfCristinPublication that = (NvaPublicationPartOfCristinPublication) o;
        return Objects.equals(getNvaPublicationIdentifier(), that.getNvaPublicationIdentifier())
               && Objects.equals(getChildPublication(), that.getChildPublication())
               && Objects.equals(getPartOf(), that.getPartOf());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getNvaPublicationIdentifier(), getChildPublication(), getPartOf());
    }
}
