package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "NvaPublicationPartOfBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class NvaPublicationPartOf {

    @JsonProperty("cristinid")
    private String cristinId;

    private Publication parentPublication;

    @JacocoGenerated
    public NvaPublicationPartOf() {
    }
}
