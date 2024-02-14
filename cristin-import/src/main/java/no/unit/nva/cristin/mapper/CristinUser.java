package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "CristinUserBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinUser {

    @JsonProperty("personlopenr")
    private String identifier;

    @JsonProperty("fornavn")
    private String firstName;

    @JsonProperty("etternavn")
    private String lastName;

    @JacocoGenerated
    @JsonCreator
    public CristinUser() {

    }

}
