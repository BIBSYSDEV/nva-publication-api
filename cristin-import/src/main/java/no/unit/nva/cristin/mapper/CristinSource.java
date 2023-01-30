package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "CristinSourceBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
// This list should be emptied by either mapping the field to an NVA field or asking the Cristin people (Daniel)
// to remove it from the exports
@JsonIgnoreProperties({"VARBEID_VDISIPLIN"})
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
