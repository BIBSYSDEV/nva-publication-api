package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "CristinSourceBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinSource {

    @JsonProperty("kildekode")
    private String sourceCode;

    @JsonProperty("kildeid")
    private String sourceIdentifier;

    @JacocoGenerated
    public CristinSource() {

    }

    @JacocoGenerated
    public CristinSourceBuilder copy() {
        return this.toBuilder();
    }
}
