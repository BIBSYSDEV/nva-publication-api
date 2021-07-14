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
        builderClassName = "CristinPresentationalWorkBuilder",
        toBuilder = true,
        builderMethodName = "builder",
        buildMethodName = "build",
        setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"personlopenr"})

public class CristinPresentationalWork {

    @JsonProperty("presentasjonslopenr")
    private Integer identifier;
    @JsonProperty("presentasjonstypekode")
    private String presentationType;


    @JacocoGenerated
    public CristinPresentationalWorkBuilder copy() {
        return this.toBuilder();
    }
}
