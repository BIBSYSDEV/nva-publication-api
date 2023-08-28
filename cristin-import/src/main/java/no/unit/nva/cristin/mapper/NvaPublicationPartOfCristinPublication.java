package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "NvaPublicationPartOfCristinPublicationBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
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

}
